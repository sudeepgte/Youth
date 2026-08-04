package com.example.demo.repository;

import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.model.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, Long> {
    List<UserReward> findByUserOrderByIssueDateDesc(User user);
    Optional<UserReward> findByUserAndEvent(User user, Event event);
    Optional<UserReward> findByRewardCode(String rewardCode);
}
