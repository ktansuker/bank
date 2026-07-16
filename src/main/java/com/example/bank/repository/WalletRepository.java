package com.example.bank.repository;

import com.example.bank.entity.Currency;
import com.example.bank.entity.User;
import com.example.bank.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserAndCurrency(User user, Currency currency);
}
