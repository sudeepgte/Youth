package com.example.demo.repository;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleGift;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BattleGiftRepository extends JpaRepository<BattleGift, Long> {
    List<BattleGift> findByBattleOrderBySentAtDesc(Battle battle);
    long countByBattle(Battle battle);
}
