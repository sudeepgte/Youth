package com.example.demo.repository;

import com.example.demo.model.Tournament;
import com.example.demo.model.TournamentParticipant;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    List<TournamentParticipant> findByTournament(Tournament tournament);
    List<TournamentParticipant> findByTournamentOrderBySeedAsc(Tournament tournament);
    boolean existsByTournamentAndUser(Tournament tournament, User user);
    TournamentParticipant findByTournamentAndUser(Tournament tournament, User user);
    long countByTournament(Tournament tournament);
}
