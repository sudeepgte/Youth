package com.example.demo.service;
import java.time.LocalDateTime;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TournamentService {

    @Autowired private TournamentRepository tournamentRepository;
    @Autowired private TournamentParticipantRepository participantRepository;
    @Autowired private TournamentRoundRepository roundRepository;
    @Autowired private TournamentBattleRepository tournamentBattleRepository;
    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleParticipantRepository battleParticipantRepository;

    @Transactional
    public Tournament createTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    @Transactional
    public void startTournament(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> new IllegalArgumentException("Invalid tournament"));
        if (!"REGISTRATION_OPEN".equals(t.getStatus()) && !"REGISTRATION_CLOSED".equals(t.getStatus())) {
            throw new IllegalStateException("Tournament not in startable state");
        }
        
        List<TournamentParticipant> participants = participantRepository.findByTournamentOrderBySeedAsc(t);
        if (participants.size() < 2) {
            throw new IllegalStateException("Need at least 2 participants");
        }

        t.setStatus("IN_PROGRESS");
        tournamentRepository.save(t);
        generateBracket(t, participants);
    }

    private void generateBracket(Tournament t, List<TournamentParticipant> participants) {
        int n = participants.size();
        int powerOfTwo = 1;
        while (powerOfTwo < n) {
            powerOfTwo *= 2;
        }

        int byes = powerOfTwo - n;
        
        // Very simplified bracket generation: 
        // We'll pad the participants list with nulls for BYEs
        List<User> players = new ArrayList<>();
        for (TournamentParticipant tp : participants) {
            players.add(tp.getUser());
        }
        for (int i = 0; i < byes; i++) {
            players.add(null); // null means a BYE
        }
        
        int totalRounds = (int) (Math.log(powerOfTwo) / Math.log(2));
        
        // Create rounds
        List<TournamentRound> rounds = new ArrayList<>();
        for (int r = 1; r <= totalRounds; r++) {
            TournamentRound round = new TournamentRound();
            round.setTournament(t);
            round.setRoundNumber(r);
            if (r == totalRounds) round.setName("Grand Final");
            else if (r == totalRounds - 1) round.setName("Semi Final");
            else if (r == totalRounds - 2) round.setName("Quarter Final");
            else round.setName("Round " + r);
            rounds.add(roundRepository.save(round));
        }

        // Build battles backwards or forwards?
        // Let's build round by round
        List<TournamentBattle> prevRoundBattles = new ArrayList<>();
        for (int r = 1; r <= totalRounds; r++) {
            TournamentRound currentRound = rounds.get(r - 1);
            int matchesInRound = powerOfTwo / (int) Math.pow(2, r);
            List<TournamentBattle> currentRoundBattles = new ArrayList<>();
            
            for (int m = 1; m <= matchesInRound; m++) {
                TournamentBattle tb = new TournamentBattle();
                tb.setTournament(t);
                tb.setRound(currentRound);
                tb.setMatchNumber(m);
                tb.setStatus("WAITING");
                
                if (r == 1) {
                    // Seed initial players
                    // Simplified: just pair adjacent for now.
                    // A real system would use a standard seeding algorithm.
                    tb.setPlayer1(players.get((m - 1) * 2));
                    tb.setPlayer2(players.get((m - 1) * 2 + 1));
                    checkAndActivateBattle(tb);
                }
                
                currentRoundBattles.add(tournamentBattleRepository.save(tb));
            }
            
            // Link previous round to this round
            if (r > 1) {
                for (int i = 0; i < prevRoundBattles.size(); i++) {
                    TournamentBattle pTb = prevRoundBattles.get(i);
                    TournamentBattle nTb = currentRoundBattles.get(i / 2);
                    pTb.setNextBattle(nTb);
                    tournamentBattleRepository.save(pTb);
                    
                    // If pTb had a BYE automatically, it might already have advanced a winner
                    if ("COMPLETED".equals(pTb.getStatus()) && pTb.getWinner() != null) {
                        advanceToNext(pTb.getWinner(), nTb, i % 2 == 0);
                    }
                }
            }
            
            prevRoundBattles = currentRoundBattles;
        }
        
        // Handle BYEs in round 1 immediately
        for (TournamentBattle tb : rounds.get(0).getBattles()) {
            if (tb.getPlayer1() == null && tb.getPlayer2() != null) {
                // Player 2 advances
                advanceWinner(tb.getId(), tb.getPlayer2().getId());
            } else if (tb.getPlayer2() == null && tb.getPlayer1() != null) {
                // Player 1 advances
                advanceWinner(tb.getId(), tb.getPlayer1().getId());
            } else if (tb.getPlayer1() == null && tb.getPlayer2() == null) {
                // Both null, shouldn't happen unless tournament is empty
            }
        }
    }

    private void checkAndActivateBattle(TournamentBattle tb) {
        if (tb.getPlayer1() != null && tb.getPlayer2() != null && "WAITING".equals(tb.getStatus())) {
            tb.setStatus("READY");
            createActualBattle(tb);
        }
    }

    private void createActualBattle(TournamentBattle tb) {
        Battle battle = new Battle();
        battle.setTitle(tb.getTournament().getName() + " - " + tb.getRound().getName() + " Match " + tb.getMatchNumber());
        battle.setCategory("Tournament");
        battle.setMaxParticipants(2);
        battle.setDurationMinutes(tb.getTournament().getBattleDurationMinutes() != null ? tb.getTournament().getBattleDurationMinutes() : 10);
        battle.setMode("ONLINE");
        battle.setStatus("ACTIVE");
        battle.setStartedAt(LocalDateTime.now());
        if (battle.getDurationMinutes() != null && battle.getDurationMinutes() > 0) {
            battle.setEndsAt(battle.getStartedAt().plusMinutes(battle.getDurationMinutes()));
        } else {
            battle.setEndsAt(battle.getStartedAt().plusHours(24));
        }
        battle = battleRepository.save(battle);
        
        tb.setBattle(battle);
        
        // Add participants
        BattleParticipant bp1 = new BattleParticipant();
        bp1.setBattle(battle);
        bp1.setUser(tb.getPlayer1());
        battleParticipantRepository.save(bp1);
        
        BattleParticipant bp2 = new BattleParticipant();
        bp2.setBattle(battle);
        bp2.setUser(tb.getPlayer2());
        battleParticipantRepository.save(bp2);
    }
    
    private void advanceToNext(User winner, TournamentBattle nextTb, boolean isPlayer1) {
        if (isPlayer1) {
            nextTb.setPlayer1(winner);
        } else {
            nextTb.setPlayer2(winner);
        }
        checkAndActivateBattle(nextTb);
        tournamentBattleRepository.save(nextTb);
    }

    @Transactional
    public void advanceWinner(Long tournamentBattleId, Long winnerId) {
        TournamentBattle tb = tournamentBattleRepository.findById(tournamentBattleId).orElse(null);
        if (tb == null || "COMPLETED".equals(tb.getStatus())) return;
        
        User winner = null;
        if (tb.getPlayer1() != null && tb.getPlayer1().getId().equals(winnerId)) winner = tb.getPlayer1();
        if (tb.getPlayer2() != null && tb.getPlayer2().getId().equals(winnerId)) winner = tb.getPlayer2();
        
        if (winner == null) return; // Invalid winner
        
        tb.setWinner(winner);
        tb.setStatus("COMPLETED");
        
        // Mark loser as eliminated
        User loser = (tb.getPlayer1() != null && tb.getPlayer1().getId().equals(winnerId)) ? tb.getPlayer2() : tb.getPlayer1();
        if (loser != null) {
            TournamentParticipant loserParticipant = participantRepository.findByTournamentAndUser(tb.getTournament(), loser);
            if (loserParticipant != null) {
                loserParticipant.setStatus("ELIMINATED");
                participantRepository.save(loserParticipant);
            }
        }
        
        tournamentBattleRepository.save(tb);
        
        if (tb.getNextBattle() != null) {
            TournamentBattle next = tb.getNextBattle();
            // Determine if player 1 or 2 based on previous round list order
            // We can just find which slot is empty
            if (next.getPlayer1() == null) {
                advanceToNext(winner, next, true);
            } else {
                advanceToNext(winner, next, false);
            }
        } else {
            // It's the grand final
            Tournament t = tb.getTournament();
            t.setWinner(winner);
            t.setStatus("COMPLETED");
            tournamentRepository.save(t);
        }
    }
}
