package com.example.demo.repository;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleLiveComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BattleLiveCommentRepository extends JpaRepository<BattleLiveComment, Long> {
    List<BattleLiveComment> findByBattleOrderBySentAtAsc(Battle battle);
    long countByBattle(Battle battle);
}
