package com.example.bank.service;

import com.example.bank.dto.TransactionHistoryDto;
import com.example.bank.dto.UserDto;
import com.example.bank.entity.Currency;
import java.math.BigDecimal;
import java.util.List;

public interface BankService {
    UserDto createUser(String username, String email, String password);

    UserDto deposit(String username, BigDecimal amount, Currency currency);

    UserDto withdraw(String username, BigDecimal amount, Currency currency);

    // targetIdentifier: kullanıcı ID'si, e-posta ya da kullanıcı adı olabilir
    UserDto transfer(String fromUsername, String targetIdentifier, BigDecimal amount, Currency currency);

    UserDto exchange(String username, Currency fromCurrency, Currency toCurrency, BigDecimal amount);

    UserDto getUserInfo(String username);

    List<UserDto> listUsers();

    List<TransactionHistoryDto> getHistory(String username);

    List<TransactionHistoryDto> getAllHistory();
}
