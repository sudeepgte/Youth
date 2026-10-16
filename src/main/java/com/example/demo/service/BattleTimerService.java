package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.repository.TournamentBattleRepository;
import com.example.demo.model.TournamentBattle;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BattleTimerService {

    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleParticipantRepository participantRepository;
    @Autowired private BattleSubmissionRepository submissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletService walletService;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private TournamentBattleRepository tournamentBattleRepository;
    @Autowired private TournamentService tournamentService;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private com.example.demo.repository.BattlePredictionRepository predictionRepository;

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

            userRepository.save(p1);
            userRepository.save(p2);
        } else {
            // Fallback for non-1v1
            battle.setStatus("COMPLETED");
        }
        
        battleRepository.save(battle);

        TournamentBattle tb = tournamentBattleRepository.findByBattle(battle);
        if (tb != null && battle.getWinner() != null) {
            tournamentService.advanceWinner(tb.getId(), battle.getWinner().getId());
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("status", "COMPLETED");
        msg.put("reason", "TIME_UP");
        messagingTemplate.convertAndSend("/topic/battle/" + battle.getId() + "/status", (Object) msg);
        
        auditLogService.log("BATTLE_END", battle.getCreator().getId(), "Battle " + battle.getId() + " ended with status " + battle.getStatus());
        
        // Send notifications
        if (participants.size() == 2) {
            User p1 = participants.get(0).getUser();
            User p2 = participants.get(1).getUser();
            BattleSubmission sub1 = submissionRepository.findByBattleAndUser(battle, p1).orElse(null);
            BattleSubmission sub2 = submissionRepository.findByBattleAndUser(battle, p2).orElse(null);
            double score1 = (sub1 != null && sub1.getVoteCount() != null) ? sub1.getVoteCount() : 0.0;
            double score2 = (sub2 != null && sub2.getVoteCount() != null) ? sub2.getVoteCount() : 0.0;

            if (score1 == score2) {
                notificationService.sendNotification(p1.getId(), "Battle Ended in a Tie", "Your battle ended in a tie.", "fas fa-handshake", "/battles/" + battle.getId());
                notificationService.sendNotification(p2.getId(), "Battle Ended in a Tie", "Your battle ended in a tie.", "fas fa-handshake", "/battles/" + battle.getId());
            } else if (score1 > score2) {
                notificationService.sendNotification(p1.getId(), "You Won!", "Congratulations, you won the battle!", "fas fa-trophy", "/battles/" + battle.getId());
                notificationService.sendNotification(p2.getId(), "You Lost", "You lost the battle. Better luck next time!", "fas fa-sad-tear", "/battles/" + battle.getId());
            } else {
                notificationService.sendNotification(p2.getId(), "You Won!", "Congratulations, you won the battle!", "fas fa-trophy", "/battles/" + battle.getId());
                notificationService.sendNotification(p1.getId(), "You Lost", "You lost the battle. Better luck next time!", "fas fa-sad-tear", "/battles/" + battle.getId());
            }
            
            Double prize = battle.getPrizePool() != null ? battle.getPrizePool() : (battle.getPrize1() != null ? battle.getPrize1() : 0.0);
            if (prize > 0) {
                if (score1 == score2) {
                    notificationService.sendNotification(p1.getId(), "Reward Received", "You received " + (prize/2) + " coins for tying.", "fas fa-coins", "/wallet");
                    notificationService.sendNotification(p2.getId(), "Reward Received", "You received " + (prize/2) + " coins for tying.", "fas fa-coins", "/wallet");
                } else if (score1 > score2) {
                    notificationService.sendNotification(p1.getId(), "Reward Received", "You received " + prize + " coins for winning!", "fas fa-coins", "/wallet");
                } else {
                    notificationService.sendNotification(p2.getId(), "Reward Received", "You received " + prize + " coins for winning!", "fas fa-coins", "/wallet");
                }
            }
        }
        
        // --- Prediction Betting Payouts ---
        if (battle.getWinner() != null && !"TIE".equals(battle.getStatus())) {
            java.util.List<com.example.demo.model.BattlePrediction> predictions = predictionRepository.findByBattle(battle);
            for (com.example.demo.model.BattlePrediction pred : predictions) {
                User bettor = pred.getBettor();
                if (pred.getPredictedWinner().getId().equals(battle.getWinner().getId())) {
                    // Won the bet! Payout 2x
                    double payout = pred.getAmount() * 2.0;
                    bettor.addWalletBalance(payout);
                    userRepository.save(bettor);
                    notificationService.sendNotification(bettor.getId(), "Prediction Won!", "Your bet on " + battle.getWinner().getUsername() + " was correct! You won " + payout + " coins.", "fas fa-coins", "/wallet");
                } else {
                    // Lost the bet
                    notificationService.sendNotification(bettor.getId(), "Prediction Lost", "Your bet on " + pred.getPredictedWinner().getUsername() + " was incorrect.", "fas fa-times-circle", "/wallet");
                }
            }
        } else if ("TIE".equals(battle.getStatus())) {
            // Refund all bets in case of a tie
            java.util.List<com.example.demo.model.BattlePrediction> predictions = predictionRepository.findByBattle(battle);
            for (com.example.demo.model.BattlePrediction pred : predictions) {
                User bettor = pred.getBettor();
                bettor.addWalletBalance(pred.getAmount());
                userRepository.save(bettor);
                notificationService.sendNotification(bettor.getId(), "Prediction Refunded", "The battle ended in a tie. Your bet of " + pred.getAmount() + " coins was refunded.", "fas fa-undo", "/wallet");
            }
        }
    }

    private static final Map<String, Integer> recentMatchups = new java.util.concurrent.ConcurrentHashMap<>();

    public void updateElo(User player1, User player2, boolean isTie, boolean p1Wins) {
        // --- Anti-Cheat: Rating manipulation protection / Multiple-account abuse detection ---
        String matchupKey = player1.getId() < player2.getId() ? 
            (player1.getId() + "_" + player2.getId()) : (player2.getId() + "_" + player1.getId());
        
        int matchCount = recentMatchups.getOrDefault(matchupKey, 0) + 1;
        recentMatchups.put(matchupKey, matchCount);

        // If they played more than 3 times, don't update ELO or give XP to prevent win-trading
        if (matchCount > 3) {
            System.out.println("ANTI-CHEAT: Blocked ELO update for potential win-trading between User " + player1.getId() + " and " + player2.getId());
            // We can optionally flag them or shadowban here
            return;
        }

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


