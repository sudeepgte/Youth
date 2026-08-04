package com.example.demo.repository;

import com.example.demo.model.FollowRequest;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    Optional<FollowRequest> findBySenderAndReceiver(User sender, User receiver);
    List<FollowRequest> findByReceiverOrderByCreatedAtDesc(User receiver);
    List<FollowRequest> findBySenderOrderByCreatedAtDesc(User sender);

    void deleteBySenderAndReceiver(User sender, User receiver);
}
