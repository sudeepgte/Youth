package com.example.demo.repository;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleLike;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleLikeRepository extends JpaRepository<BattleLike, Long> {
    long countByBattle(Battle battle);
    boolean existsByBattleAndUser(Battle battle, User user);
}
