package com.example.demo.repository;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleSubmission;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BattleSubmissionRepository extends JpaRepository<BattleSubmission, Long> {
    List<BattleSubmission> findByBattleOrderByVoteCountDesc(Battle battle);
    boolean existsByBattleAndUser(Battle battle, User user);
    java.util.Optional<BattleSubmission> findByBattleAndUser(Battle battle, User user);
}
