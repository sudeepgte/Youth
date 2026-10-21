package com.example.demo.repository;

import com.example.demo.model.CoinTransaction;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {
    List<CoinTransaction> findByUserOrderByTimestampDesc(User user);
    boolean existsByUserAndSourceAndReason(User user, String source, String reason);
    long countByUserAndSourceAndTimestampAfter(User user, String source, java.time.LocalDateTime timestamp);
}
