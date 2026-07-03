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
}
