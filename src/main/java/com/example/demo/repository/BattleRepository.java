package com.example.demo.repository;

import com.example.demo.model.Battle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BattleRepository extends JpaRepository<Battle, Long> {
    Optional<Battle> findByRoomCode(String roomCode);
    List<Battle> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
    List<Battle> findAllByOrderByCreatedAtDesc();
    List<Battle> findByStatusOrderByCreatedAtDesc(String status);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(b) FROM Battle b WHERE b.winner = :user OR b.winner2 = :user OR b.winner3 = :user")
    long countBattlesWonByUser(@org.springframework.data.repository.query.Param("user") com.example.demo.model.User user);
}
