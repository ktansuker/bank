package com.example.bank.repository;

import com.example.bank.entity.TransactionHistory;
import com.example.bank.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    // Sıralama artık metod adından değil, çağıran taraftan gelen Pageable'ın
    // içindeki Sort bilgisinden geliyor (bkz. BankServiceImpl - Sort.by("timestamp").descending()).
    Page<TransactionHistory> findByUser(User user, Pageable pageable);

    // findAll(Pageable) zaten JpaRepository'den miras alınıyor - admin'in "tüm işlemler"
    // sorgusu için ayrıca bir metod yazmaya gerek yok, doğrudan onu kullanıyoruz.
}
