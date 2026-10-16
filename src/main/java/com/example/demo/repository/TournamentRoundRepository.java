package com.example.demo.repository;

import com.example.demo.model.Tournament;
import com.example.demo.model.TournamentRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRoundRepository extends JpaRepository<TournamentRound, Long> {
    List<TournamentRound> findByTournamentOrderByRoundNumberAsc(Tournament tournament);
}
