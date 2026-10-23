package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.WalletRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletRequestRepository extends JpaRepository<WalletRequest, Long> {
    List<WalletRequest> findByTargetUserAndStatusOrderByCreatedAtDesc(User targetUser, String status);
    List<WalletRequest> findByTargetUserOrderByCreatedAtDesc(User targetUser);
    List<WalletRequest> findByRequesterOrderByCreatedAtDesc(User requester);
}
