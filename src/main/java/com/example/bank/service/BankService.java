package com.example.bank.service;

import com.example.bank.dto.TransactionHistoryDto;
import com.example.bank.dto.UserDto;
import com.example.bank.entity.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface BankService {
    UserDto createUser(String username, String email, String password);

    UserDto deposit(String username, BigDecimal amount, Currency currency);

    UserDto withdraw(String username, BigDecimal amount, Currency currency);

    // targetIdentifier: kullanıcı ID'si, e-posta ya da kullanıcı adı olabilir
    UserDto transfer(String fromUsername, String targetIdentifier, BigDecimal amount, Currency currency);

    UserDto exchange(String username, Currency fromCurrency, Currency toCurrency, BigDecimal amount);

    UserDto getUserInfo(String username);

    Page<UserDto> listUsers(Pageable pageable);

    Page<TransactionHistoryDto> getHistory(String username, Pageable pageable);

    Page<TransactionHistoryDto> getAllHistory(Pageable pageable);
}
