package com.example.demo.repository;

import com.example.demo.model.Battle;
import com.example.demo.model.Tournament;
import com.example.demo.model.TournamentBattle;
import com.example.demo.model.TournamentRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentBattleRepository extends JpaRepository<TournamentBattle, Long> {
    List<TournamentBattle> findByTournament(Tournament tournament);
    List<TournamentBattle> findByRoundOrderByMatchNumberAsc(TournamentRound round);
    TournamentBattle findByBattle(Battle battle);
}
