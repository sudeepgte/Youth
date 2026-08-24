package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BattleTimerService {

    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleParticipantRepository participantRepository;
    @Autowired private BattleSubmissionRepository submissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletService walletService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 2000)
    public void checkBattleTimers() {
        LocalDateTime now = LocalDateTime.now();
        List<Battle> activeBattles = battleRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        
        for (Battle battle : activeBattles) {
            if (battle.getEndsAt() != null && battle.getEndsAt().isBefore(now)) {
                completeLiveBattle(battle);
            }
        }
    }

    @Transactional
    public void completeLiveBattle(Battle battle) {
        // Prevent double completion
        if (!"ACTIVE".equals(battle.getStatus())) return;

        battle.setIsLive(false);

        List<BattleParticipant> participants = participantRepository.findByBattle(battle);
        if (participants.size() == 2) {
            User p1 = participants.get(0).getUser();
            User p2 = participants.get(1).getUser();
            
            // Get Submissions to get vote counts
            BattleSubmission sub1 = submissionRepository.findByBattleAndUser(battle, p1).orElse(null);
            BattleSubmission sub2 = submissionRepository.findByBattleAndUser(battle, p2).orElse(null);

            double score1 = (sub1 != null && sub1.getVoteCount() != null) ? sub1.getVoteCount() : 0.0;
            double score2 = (sub2 != null && sub2.getVoteCount() != null) ? sub2.getVoteCount() : 0.0;

            boolean isTie = score1 == score2;
            boolean p1Wins = score1 > score2;

            if (isTie) {
                battle.setStatus("TIE");
                battle.setWinner(p1);
                battle.setWinner2(p2);
            } else {
                battle.setStatus("COMPLETED");
                if (p1Wins) {
                    battle.setWinner(p1);
                    battle.setWinner2(p2);
                } else {
                    battle.setWinner(p2);
                    battle.setWinner2(p1);
                }
            }

            // Gamification and ELO
            updateElo(p1, p2, isTie, p1Wins);

            // Prize Distribution
            Double prize = battle.getPrizePool() != null ? battle.getPrizePool() : (battle.getPrize1() != null ? battle.getPrize1() : 0.0);
            if (prize > 0) {
                if (isTie) {
                    walletService.processTransaction(p1, prize / 2.0, "COINS", "CREDIT", "Battle Tie Prize", "BATTLE_" + battle.getId());
                    walletService.processTransaction(p2, prize / 2.0, "COINS", "CREDIT", "Battle Tie Prize", "BATTLE_" + battle.getId());
                } else {
                    User winner = p1Wins ? p1 : p2;
                    walletService.processTransaction(winner, prize, "COINS", "CREDIT", "Battle Victory Prize", "BATTLE_" + battle.getId());
                }
            }

            userRepository.save(p1);
            userRepository.save(p2);
        } else {
            // Fallback for non-1v1
            battle.setStatus("COMPLETED");
        }
        
        battleRepository.save(battle);

        Map<String, Object> msg = new HashMap<>();
        msg.put("status", "COMPLETED");
        msg.put("reason", "TIME_UP");
        messagingTemplate.convertAndSend("/topic/battle/" + battle.getId() + "/status", (Object) msg);
    }

    private void updateElo(User player1, User player2, boolean isTie, boolean p1Wins) {
        int k = 32;
        double p1Rating = player1.getBattleRating() != null ? player1.getBattleRating() : 1500;
        double p2Rating = player2.getBattleRating() != null ? player2.getBattleRating() : 1500;

        double expected1 = 1.0 / (1 + Math.pow(10, (p2Rating - p1Rating) / 400.0));
        double expected2 = 1.0 / (1 + Math.pow(10, (p1Rating - p2Rating) / 400.0));

        double actual1 = isTie ? 0.5 : (p1Wins ? 1.0 : 0.0);
        double actual2 = isTie ? 0.5 : (p1Wins ? 0.0 : 1.0);

        int newR1 = (int) Math.round(p1Rating + k * (actual1 - expected1));
        int newR2 = (int) Math.round(p2Rating + k * (actual2 - expected2));

        player1.setBattleRating(newR1);
        player2.setBattleRating(newR2);

        player1.setXp((player1.getXp() != null ? player1.getXp() : 0) + (isTie ? 50 : (p1Wins ? 100 : 25)));
        player2.setXp((player2.getXp() != null ? player2.getXp() : 0) + (isTie ? 50 : (p1Wins ? 25 : 100)));

        if (p1Wins) {
            player1.setBattleWins((player1.getBattleWins() != null ? player1.getBattleWins() : 0) + 1);
            player2.setBattleLosses((player2.getBattleLosses() != null ? player2.getBattleLosses() : 0) + 1);
            player1.setWinStreak((player1.getWinStreak() != null ? player1.getWinStreak() : 0) + 1);
            player2.setWinStreak(0);
        } else if (!isTie) {
            player2.setBattleWins((player2.getBattleWins() != null ? player2.getBattleWins() : 0) + 1);
            player1.setBattleLosses((player1.getBattleLosses() != null ? player1.getBattleLosses() : 0) + 1);
            player2.setWinStreak((player2.getWinStreak() != null ? player2.getWinStreak() : 0) + 1);
            player1.setWinStreak(0);
        } else {
            player1.setBattleDraws((player1.getBattleDraws() != null ? player1.getBattleDraws() : 0) + 1);
            player2.setBattleDraws((player2.getBattleDraws() != null ? player2.getBattleDraws() : 0) + 1);
            player1.setWinStreak(0);
            player2.setWinStreak(0);
        }
    }
}
