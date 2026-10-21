package com.example.demo.repository;

import com.example.demo.model.TournamentBattle;
import com.example.demo.model.TournamentJudgeScore;
import com.example.demo.model.TournamentParticipant;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentJudgeScoreRepository extends JpaRepository<TournamentJudgeScore, Long> {
    List<TournamentJudgeScore> findByTournamentBattle(TournamentBattle battle);
    Optional<TournamentJudgeScore> findByTournamentBattleAndJudgeAndParticipant(TournamentBattle battle, User judge, TournamentParticipant participant);
    List<TournamentJudgeScore> findByTournamentBattleAndParticipant(TournamentBattle battle, TournamentParticipant participant);
}
