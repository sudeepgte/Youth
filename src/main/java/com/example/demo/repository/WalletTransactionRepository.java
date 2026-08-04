package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserOrderByTimestampDesc(User user);
}
