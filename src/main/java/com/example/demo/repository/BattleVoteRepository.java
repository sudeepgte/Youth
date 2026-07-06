package com.example.demo.repository;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleVote;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleVoteRepository extends JpaRepository<BattleVote, Long> {
    boolean existsByBattleAndVoter(Battle battle, User voter);
}
