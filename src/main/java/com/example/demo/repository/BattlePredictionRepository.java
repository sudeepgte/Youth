package com.example.demo.repository;
import com.example.demo.model.Battle;
import com.example.demo.model.BattlePrediction;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface BattlePredictionRepository extends JpaRepository<BattlePrediction, Long> {
    List<BattlePrediction> findByBattle(Battle battle);
    boolean existsByBattleAndBettor(Battle battle, User bettor);
    BattlePrediction findByBattleAndBettor(Battle battle, User bettor);
}