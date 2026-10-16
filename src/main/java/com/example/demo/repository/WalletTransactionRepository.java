package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserOrderByTimestampDesc(User user);
    List<WalletTransaction> findByUserOrderByCreatedAtDesc(User user);
}