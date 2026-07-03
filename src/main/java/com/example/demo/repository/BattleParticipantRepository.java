package com.example.demo.repository;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleParticipant;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, Long> {
    List<BattleParticipant> findByBattle(Battle battle);
    boolean existsByBattleAndUser(Battle battle, User user);
    long countByBattle(Battle battle);
}
