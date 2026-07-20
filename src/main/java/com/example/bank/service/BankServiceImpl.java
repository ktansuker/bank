package com.example.bank.service;

import com.example.bank.dto.TransactionHistoryDto;
import com.example.bank.dto.UserDto;
import com.example.bank.dto.WalletDto;
import com.example.bank.entity.*;
import com.example.bank.exception.BankException;
import com.example.bank.messaging.TransactionEventPublisher;
import com.example.bank.repository.TransactionHistoryRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.repository.WalletRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class BankServiceImpl implements BankService {

    private static final long LOCK_WAIT_SECONDS = 5;
    private static final long LOCK_LEASE_SECONDS = 10;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExchangeRateService exchangeRateService;
    private final RedissonClient redissonClient;
    private final TransactionEventPublisher transactionEventPublisher;

    public BankServiceImpl(UserRepository userRepository, WalletRepository walletRepository,
            TransactionHistoryRepository historyRepository, PasswordEncoder passwordEncoder,
            ExchangeRateService exchangeRateService, RedissonClient redissonClient,
            TransactionEventPublisher transactionEventPublisher) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
        this.exchangeRateService = exchangeRateService;
        this.redissonClient = redissonClient;
        this.transactionEventPublisher = transactionEventPublisher;
    }

    @Override
    @Transactional
    @CacheEvict(value = "userList", allEntries = true)
    public UserDto createUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new BankException("Bu kullanıcı adı zaten alınmış!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BankException("Bu e-posta adresi zaten kullanılıyor!");
        }

        User user = new User(username, email, passwordEncoder.encode(password), "ROLE_USER");
        for (Currency currency : Currency.values()) {
            user.addWallet(new Wallet(user, currency));
        }

        return convertToDto(userRepository.save(user));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userInfo", key = "#username"),
            @CacheEvict(value = "userList", allEntries = true)
    })
    public UserDto deposit(String username, BigDecimal amount, Currency currency) {
        validateAmount(amount);
        User user = findUserOrThrow(username);

        return withLock(walletLockKey(username, currency), () -> {
            Wallet wallet = findWalletOrThrow(user, currency);
            BigDecimal newBalance = wallet.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP);
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);

            TransactionHistory history = logHistory(user, TransactionType.DEPOSIT, currency, amount, newBalance,
                    null, amount + " " + currency + " yatırıldı");
            UserDto dto = convertToDto(user);

            transactionEventPublisher.publish(convertHistoryToDto(history));
            return dto;
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userInfo", key = "#username"),
            @CacheEvict(value = "userList", allEntries = true)
    })
    public UserDto withdraw(String username, BigDecimal amount, Currency currency) {
        validateAmount(amount);
        User user = findUserOrThrow(username);

        return withLock(walletLockKey(username, currency), () -> {
            Wallet wallet = findWalletOrThrow(user, currency);
            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new BankException("Yetersiz bakiye! Mevcut bakiye: " + wallet.getBalance() + " " + currency);
            }

            BigDecimal newBalance = wallet.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP);
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);

            TransactionHistory history = logHistory(user, TransactionType.WITHDRAW, currency, amount, newBalance,
                    null, amount + " " + currency + " çekildi");
            UserDto dto = convertToDto(user);

            transactionEventPublisher.publish(convertHistoryToDto(history));
            return dto;
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userInfo", allEntries = true),
            @CacheEvict(value = "userList", allEntries = true)
    })
    public UserDto transfer(String fromUsername, String targetIdentifier, BigDecimal amount, Currency currency) {
        validateAmount(amount);

        User sender = findUserOrThrow(fromUsername);
        User receiver = resolveTargetUser(targetIdentifier);

        if (sender.getId().equals(receiver.getId())) {
            throw new BankException("Kendinize transfer yapamazsınız");
        }

        return withTwoLocks(
                walletLockKey(sender.getUsername(), currency),
                walletLockKey(receiver.getUsername(), currency),
                () -> {
                    Wallet senderWallet = findWalletOrThrow(sender, currency);
                    Wallet receiverWallet = findWalletOrThrow(receiver, currency);

                    if (senderWallet.getBalance().compareTo(amount) < 0) {
                        throw new BankException(
                                "Yetersiz bakiye! Mevcut bakiye: " + senderWallet.getBalance() + " " + currency);
                    }

                    BigDecimal senderNewBalance = senderWallet.getBalance().subtract(amount).setScale(2,
                            RoundingMode.HALF_UP);
                    BigDecimal receiverNewBalance = receiverWallet.getBalance().add(amount).setScale(2,
                            RoundingMode.HALF_UP);

                    senderWallet.setBalance(senderNewBalance);
                    receiverWallet.setBalance(receiverNewBalance);
                    walletRepository.save(senderWallet);
                    walletRepository.save(receiverWallet);

                    TransactionHistory outHistory = logHistory(sender, TransactionType.TRANSFER_OUT, currency, amount,
                            senderNewBalance, receiver.getUsername(),
                            amount + " " + currency + " " + receiver.getUsername() + " kullanıcısına gönderildi");
                    TransactionHistory inHistory = logHistory(receiver, TransactionType.TRANSFER_IN, currency, amount,
                            receiverNewBalance, sender.getUsername(),
                            amount + " " + currency + " " + sender.getUsername() + " kullanıcısından alındı");

                    UserDto senderDto = convertToDto(sender);

                    transactionEventPublisher.publish(convertHistoryToDto(outHistory));
                    transactionEventPublisher.publish(convertHistoryToDto(inHistory));

                    return senderDto;
                });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userInfo", key = "#username"),
            @CacheEvict(value = "userList", allEntries = true)
    })
    public UserDto exchange(String username, Currency fromCurrency, Currency toCurrency, BigDecimal amount) {
        validateAmount(amount);
        if (fromCurrency == toCurrency) {
            throw new BankException("Aynı para birimine dönüşüm yapılamaz");
        }

        User user = findUserOrThrow(username);

        return withTwoLocks(
                walletLockKey(username, fromCurrency),
                walletLockKey(username, toCurrency),
                () -> {
                    Wallet fromWallet = findWalletOrThrow(user, fromCurrency);
                    Wallet toWallet = findWalletOrThrow(user, toCurrency);

                    if (fromWallet.getBalance().compareTo(amount) < 0) {
                        throw new BankException(
                                "Yetersiz bakiye! Mevcut bakiye: " + fromWallet.getBalance() + " " + fromCurrency);
                    }

                    BigDecimal convertedAmount = exchangeRateService.convert(amount, fromCurrency, toCurrency);

                    BigDecimal fromNewBalance = fromWallet.getBalance().subtract(amount).setScale(2,
                            RoundingMode.HALF_UP);
                    BigDecimal toNewBalance = toWallet.getBalance().add(convertedAmount).setScale(2,
                            RoundingMode.HALF_UP);

                    fromWallet.setBalance(fromNewBalance);
                    toWallet.setBalance(toNewBalance);
                    walletRepository.save(fromWallet);
                    walletRepository.save(toWallet);

                    String note = amount + " " + fromCurrency + " → " + convertedAmount + " " + toCurrency
                            + " olarak bozduruldu";
                    TransactionHistory outHistory = logHistory(user, TransactionType.EXCHANGE_OUT, fromCurrency, amount,
                            fromNewBalance, null, note);
                    TransactionHistory inHistory = logHistory(user, TransactionType.EXCHANGE_IN, toCurrency,
                            convertedAmount, toNewBalance, null, note);

                    UserDto dto = convertToDto(user);
                    transactionEventPublisher.publish(convertHistoryToDto(outHistory));
                    transactionEventPublisher.publish(convertHistoryToDto(inHistory));

                    return dto;
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userInfo", key = "#username")
    public UserDto getUserInfo(String username) {
        return convertToDto(findUserOrThrow(username));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userList")
    public List<UserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionHistoryDto> getHistory(String username) {
        User user = findUserOrThrow(username);
        return historyRepository.findByUserOrderByTimestampDesc(user).stream()
                .map(this::convertHistoryToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionHistoryDto> getAllHistory() {
        return historyRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::convertHistoryToDto)
                .collect(Collectors.toList());
    }

    private User resolveTargetUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new BankException("Alıcı bilgisi boş olamaz");
        }
        String trimmed = identifier.trim();

        if (trimmed.matches("\\d+")) {
            return userRepository.findById(Long.valueOf(trimmed))
                    .orElseThrow(() -> new BankException("ID ile kullanıcı bulunamadı: " + trimmed));
        }

        // "@" içeriyorsa e-posta olarak dene
        if (trimmed.contains("@")) {
            return userRepository.findByEmail(trimmed)
                    .orElseThrow(() -> new BankException("Bu e-posta ile kullanıcı bulunamadı: " + trimmed));
        }

        // Aksi halde kullanıcı adı olarak dene
        return userRepository.findByUsername(trimmed)
                .orElseThrow(() -> new BankException("Kullanıcı bulunamadı: " + trimmed));
    }

    private String walletLockKey(String username, Currency currency) {
        return "lock:wallet:" + username + ":" + currency;
    }

    private <T> T withLock(String key, Supplier<T> action) {
        RLock lock = acquireLock(key);
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private <T> T withTwoLocks(String keyA, String keyB, Supplier<T> action) {
        String firstKey = keyA.compareTo(keyB) <= 0 ? keyA : keyB;
        String secondKey = keyA.compareTo(keyB) <= 0 ? keyB : keyA;

        RLock lock1 = acquireLock(firstKey);
        RLock lock2 = acquireLock(secondKey);
        try {
            return action.get();
        } finally {
            lock2.unlock();
            lock1.unlock();
        }
    }

    private RLock acquireLock(String key) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BankException("İşlem sırasında beklenmedik bir kesinti oluştu, tekrar deneyin");
        }
        if (!acquired) {
            throw new BankException(
                    "Hesabınız şu anda başka bir işlemde kullanılıyor, lütfen birkaç saniye sonra tekrar deneyin");
        }
        return lock;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Geçersiz miktar!");
        }
    }

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BankException("Kullanıcı bulunamadı"));
    }

    private Wallet findWalletOrThrow(User user, Currency currency) {
        return walletRepository.findByUserAndCurrency(user, currency)
                .orElseThrow(() -> new BankException("Bu para birimi için cüzdan bulunamadı: " + currency));
    }

    private TransactionHistory logHistory(User user, TransactionType type, Currency currency, BigDecimal amount,
            BigDecimal balanceAfter, String counterparty, String note) {
        return historyRepository
                .save(new TransactionHistory(user, type, currency, amount, balanceAfter, counterparty, note));
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setWallets(user.getWallets().stream()
                .map(w -> new WalletDto(w.getCurrency(), w.getBalance()))
                .collect(Collectors.toList()));
        return dto;
    }

    private TransactionHistoryDto convertHistoryToDto(TransactionHistory h) {
        TransactionHistoryDto dto = new TransactionHistoryDto();
        dto.setUsername(h.getUser().getUsername());
        dto.setType(h.getType());
        dto.setCurrency(h.getCurrency());
        dto.setAmount(h.getAmount());
        dto.setBalanceAfter(h.getBalanceAfter());
        dto.setCounterparty(h.getCounterparty());
        dto.setNote(h.getNote());
        dto.setTimestamp(h.getTimestamp());
        return dto;
    }
}
