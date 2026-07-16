package com.example.bank.repository;

import com.example.bank.entity.TransactionHistory;
import com.example.bank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    List<TransactionHistory> findByUserOrderByTimestampDesc(User user);

    List<TransactionHistory> findAllByOrderByTimestampDesc();
}
