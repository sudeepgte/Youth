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
    @Autowired private CoinTransactionRepository coinTransactionRepository;
    @Autowired private WalletService walletService;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private TournamentBattleRepository tournamentBattleRepository;
    @Autowired private TournamentService tournamentService;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private com.example.demo.repository.BattlePredictionRepository predictionRepository;
    @Autowired private EmailService emailService;

    @Scheduled(fixedRate = 2000)
    public void checkBattleTimers() {
        LocalDateTime now = LocalDateTime.now();
        List<Battle> activeBattles = battleRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        for (Battle battle : activeBattles) {
            if (battle.getEndsAt() != null && battle.getEndsAt().isBefore(now)) {
                completeLiveBattle(battle);
            }
        }

        List<Battle> votingBattles = battleRepository.findByStatusOrderByCreatedAtDesc("VOTING");
        for (Battle battle : votingBattles) {
            LocalDateTime end = battle.getVotingEndsAt() != null ? battle.getVotingEndsAt() : battle.getEndsAt();
            if (end != null && end.isBefore(now)) {
                completeLiveBattle(battle);
            }
        }
    }

    private int determineCoinStake(Battle battle) {
        if (battle.getPrize1() != null && battle.getPrize1() > 0) {
            return Math.max(50, (int) Math.round(battle.getPrize1()));
        }
        if (battle.getEntryFee() != null && battle.getEntryFee() > 0) {
            return Math.max(50, (int) Math.round(battle.getEntryFee()));
        }
        if (battle.getPrizePool() != null && battle.getPrizePool() > 0) {
            return Math.max(50, (int) Math.round(battle.getPrizePool()));
        }
        return 50; // default friendly battle stake
    }

    private int transferCoinsFromLoserToWinner(User loser, User winner, int stakeAmount, Battle battle, String reason) {
        if (loser == null || winner == null || loser.getId().equals(winner.getId())) return 0;

        loser = userRepository.findById(loser.getId()).orElse(loser);
        int loserBalance = loser.getCoins() != null ? loser.getCoins() : 0;
        int coinsToDeduct = Math.min(loserBalance, stakeAmount);

        // Deduct coins from loser
        loser.deductCoins(stakeAmount);
        userRepository.save(loser);

        if (coinsToDeduct > 0) {
            coinTransactionRepository.save(new CoinTransaction(
                loser,
                -coinsToDeduct,
                "Battle Defeat (#" + battle.getId() + ")",
                "Coins transferred to @" + winner.getUsername() + " (" + reason + ")"
            ));
        }

        notificationService.sendNotification(
            loser.getId(),
            "Coins Deducted - Battle Defeat",
            "You lost battle #" + battle.getId() + " (" + battle.getTitle() + "). " + 
            coinsToDeduct + " coins were deducted and transferred to @" + winner.getUsername() + ".",
            "fas fa-minus-circle",
            "/battles/" + battle.getId()
        );

        return coinsToDeduct;
    }

    private void awardBattleRewards(User user, Double cashAmount, int xpAmount, Battle battle, String rankDesc) {
        if (user == null) return;
        user = userRepository.findById(user.getId()).orElse(user);

        // 1. Award XP and update level automatically
        user.addXp(xpAmount);

        // 2. Award Cash
        if (cashAmount != null && cashAmount > 0) {
            walletService.processTransaction(user, cashAmount, "INR", "CREDIT", 
                "Battle Prize (" + rankDesc + ") - " + battle.getTitle() + " (#" + battle.getId() + ")", 
                "BATTLE_PRIZE_" + battle.getId() + "_" + user.getId() + "_" + System.currentTimeMillis());
        }
        userRepository.save(user);
    }

    @Transactional
    public void completeLiveBattle(Battle battle) {
        // Prevent double completion
        if (!"ACTIVE".equals(battle.getStatus()) && !"VOTING".equals(battle.getStatus())) return;

        battle.setIsLive(false);

        List<BattleParticipant> participants = participantRepository.findByBattle(battle);
        if (participants.isEmpty()) {
            battle.setStatus("COMPLETED");
            battleRepository.save(battle);
            return;
        }

        User winner = null;
        User runnerUp = null;
        boolean isTie = false;

        if (participants.size() == 2) {
            User p1 = participants.get(0).getUser();
            User p2 = participants.get(1).getUser();
            
            // Get Submissions to get vote counts
            BattleSubmission sub1 = submissionRepository.findByBattleAndUser(battle, p1).orElse(null);
            BattleSubmission sub2 = submissionRepository.findByBattleAndUser(battle, p2).orElse(null);

            double score1 = (sub1 != null && sub1.getVoteCount() != null) ? sub1.getVoteCount() : 0.0;
            double score2 = (sub2 != null && sub2.getVoteCount() != null) ? sub2.getVoteCount() : 0.0;

            isTie = (score1 == score2);
            boolean p1Wins = score1 > score2;

            if (isTie) {
                battle.setStatus("TIE");
                battle.setWinner(p1);
                battle.setWinner2(p2);
                winner = p1;
                runnerUp = p2;
            } else {
                battle.setStatus("COMPLETED");
                if (p1Wins) {
                    battle.setWinner(p1);
                    battle.setWinner2(p2);
                    winner = p1;
                    runnerUp = p2;
                } else {
                    battle.setWinner(p2);
                    battle.setWinner2(p1);
                    winner = p2;
                    runnerUp = p1;
                }
            }

            // Gamification and ELO
            updateElo(p1, p2, isTie, p1Wins);
        } else {
            // Multi-participant battle
            battle.setStatus("COMPLETED");
            List<BattleSubmission> subs = submissionRepository.findByBattleOrderByVoteCountDesc(battle);
            if (!subs.isEmpty()) {
                winner = subs.get(0).getUser();
                battle.setWinner(winner);
                if (subs.size() > 1) {
                    runnerUp = subs.get(1).getUser();
                    battle.setWinner2(runnerUp);
                }
                if (subs.size() > 2) {
                    battle.setWinner3(subs.get(2).getUser());
                }
            } else if (!participants.isEmpty()) {
                winner = participants.get(0).getUser();
                battle.setWinner(winner);
                if (participants.size() > 1) {
                    runnerUp = participants.get(1).getUser();
                    battle.setWinner2(runnerUp);
                }
            }
        }
        
        battleRepository.save(battle);

        // Award prizes, XP, and coins to winners
        int winXp = battle.getWinnerXp() != null && battle.getWinnerXp() > 0 ? battle.getWinnerXp() : 500;
        Double prize1 = (battle.getPrize1() != null && battle.getPrize1() > 0) ? battle.getPrize1() 
                : (battle.getPrizePool() != null ? battle.getPrizePool() : 0.0);
        Double prize2 = battle.getPrize2() != null ? battle.getPrize2() : 0.0;
        int coinStake = determineCoinStake(battle);

        if (isTie && winner != null && runnerUp != null) {
            double splitPrize = prize1 > 0 ? (Math.round((prize1 / 2.0) * 100.0) / 100.0) : 0.0;
            int tieXp = (int) Math.round(winXp * 0.5);

            awardBattleRewards(winner, splitPrize, tieXp, battle, "Tie (Split Prize)");
            awardBattleRewards(runnerUp, splitPrize, tieXp, battle, "Tie (Split Prize)");

            notificationService.sendNotification(winner.getId(), "Battle Ended in a Tie", "Battle tied! You received ₹" + splitPrize + " cash and " + tieXp + " XP.", "fas fa-handshake", "/battles/" + battle.getId());
            notificationService.sendNotification(runnerUp.getId(), "Battle Ended in a Tie", "Battle tied! You received ₹" + splitPrize + " cash and " + tieXp + " XP.", "fas fa-handshake", "/battles/" + battle.getId());
        } else {
            int totalCoinsFromLosers = 0;

            if (participants.size() == 2 && winner != null && runnerUp != null) {
                // 1v1 Battle: Deduct coins from runnerUp (the loser) and transfer to winner
                runnerUp = userRepository.findById(runnerUp.getId()).orElse(runnerUp);
                int deducted = transferCoinsFromLoserToWinner(runnerUp, winner, coinStake, battle, "1v1 Battle");
                totalCoinsFromLosers += deducted;

                runnerUp.setBattleLosses((runnerUp.getBattleLosses() != null ? runnerUp.getBattleLosses() : 0) + 1);
                runnerUp.setWinStreak(0);
                userRepository.save(runnerUp);
            } else if (winner != null) {
                // Multi-participant battle: Deduct coins from ALL participants who lost to the winner
                for (BattleParticipant bp : participants) {
                    if (bp.getUser() != null && !bp.getUser().getId().equals(winner.getId())) {
                        User loserUser = userRepository.findById(bp.getUser().getId()).orElse(bp.getUser());
                        int deducted = transferCoinsFromLoserToWinner(loserUser, winner, coinStake, battle, "Arena Battle");
                        totalCoinsFromLosers += deducted;

                        loserUser.setBattleLosses((loserUser.getBattleLosses() != null ? loserUser.getBattleLosses() : 0) + 1);
                        loserUser.setWinStreak(0);
                        loserUser.addXp(50);
                        userRepository.save(loserUser);
                    }
                }
            }

            // 1st Prize Winner Rewards
            if (winner != null) {
                winner = userRepository.findById(winner.getId()).orElse(winner);
                winner.addXp(winXp);
                winner.setBattleWins((winner.getBattleWins() != null ? winner.getBattleWins() : 0) + 1);
                winner.setWinStreak((winner.getWinStreak() != null ? winner.getWinStreak() : 0) + 1);

                // Coins awarded: transferred from loser(s), guaranteed at least coinStake
                int coinsAwarded = Math.max(totalCoinsFromLosers, coinStake);
                winner.addCoins(coinsAwarded);

                coinTransactionRepository.save(new CoinTransaction(
                    winner, 
                    coinsAwarded, 
                    "Battle Victory (#" + battle.getId() + ")", 
                    "Won from opponents in " + battle.getTitle()
                ));

                if (prize1 > 0) {
                    walletService.processTransaction(winner, prize1, "INR", "CREDIT", 
                        "Battle 1st Prize - " + battle.getTitle() + " (#" + battle.getId() + ")", 
                        "BATTLE_PRIZE_" + battle.getId() + "_" + winner.getId() + "_" + System.currentTimeMillis());
                }

                userRepository.save(winner);

                notificationService.sendNotification(winner.getId(), "🏆 You Won the Battle!", 
                    "Congratulations! You won " + coinsAwarded + " coins (deducted from your opponent" + (participants.size() > 2 ? "s" : "") + "), ₹" + prize1 + " cash, and " + winXp + " XP!", 
                    "fas fa-trophy", "/battles/" + battle.getId());
            }

            // 2nd Prize Runner-Up (if multi-participant with prize2)
            if (participants.size() > 2 && runnerUp != null && prize2 > 0) {
                runnerUp = userRepository.findById(runnerUp.getId()).orElse(runnerUp);
                int runnerUpXp = (int) Math.round(winXp * 0.5);
                runnerUp.addXp(runnerUpXp);
                walletService.processTransaction(runnerUp, prize2, "INR", "CREDIT", 
                    "Battle 2nd Prize - " + battle.getTitle() + " (#" + battle.getId() + ")", 
                    "BATTLE_PRIZE2_" + battle.getId() + "_" + runnerUp.getId() + "_" + System.currentTimeMillis());
                userRepository.save(runnerUp);

                notificationService.sendNotification(runnerUp.getId(), "🥈 2nd Place in Battle", 
                    "You placed 2nd! You received ₹" + prize2 + " cash prize and " + runnerUpXp + " XP!", 
                    "fas fa-medal", "/battles/" + battle.getId());
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
                    try { emailService.sendBattlePredictionWinNotification(bettor, battle.getTitle(), (int) payout); } catch (Exception ignored) {}
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

    @Transactional
    public void completeBattleWithForfeit(Battle battle, User leaver) {
        if (!"ACTIVE".equals(battle.getStatus()) && !"VOTING".equals(battle.getStatus())) return;

        battle.setIsLive(false);
        battle.setStatus("COMPLETED");

        List<BattleParticipant> participants = participantRepository.findByBattle(battle);
        User winner = null;
        for (BattleParticipant p : participants) {
            if (p.getUser() != null && !p.getUser().getId().equals(leaver.getId())) {
                winner = p.getUser();
                break;
            }
        }

        if (winner != null) {
            battle.setWinner(winner);
            battle.setWinner2(leaver);

            // Update ELO rating: winner wins, leaver loses
            updateElo(winner, leaver, false, true);

            int coinStake = determineCoinStake(battle);
            int winXp = battle.getWinnerXp() != null && battle.getWinnerXp() > 0 ? battle.getWinnerXp() : 500;
            Double prize = (battle.getPrize1() != null && battle.getPrize1() > 0) ? battle.getPrize1() 
                    : (battle.getPrizePool() != null ? battle.getPrizePool() : 0.0);

            // Deduct coins from leaver (the loser) and transfer to winner
            leaver = userRepository.findById(leaver.getId()).orElse(leaver);
            int coinsDeducted = transferCoinsFromLoserToWinner(leaver, winner, coinStake, battle, "Forfeit");
            leaver.setBattleLosses((leaver.getBattleLosses() != null ? leaver.getBattleLosses() : 0) + 1);
            leaver.setWinStreak(0);
            userRepository.save(leaver);

            // Winner gains coins from loser + XP + Cash prize
            winner = userRepository.findById(winner.getId()).orElse(winner);
            int coinsAwarded = Math.max(coinsDeducted, coinStake);
            winner.addCoins(coinsAwarded);
            winner.addXp(winXp);
            winner.setBattleWins((winner.getBattleWins() != null ? winner.getBattleWins() : 0) + 1);
            winner.setWinStreak((winner.getWinStreak() != null ? winner.getWinStreak() : 0) + 1);

            coinTransactionRepository.save(new CoinTransaction(
                winner,
                coinsAwarded,
                "Battle Won by Forfeit (#" + battle.getId() + ")",
                "Coins transferred from @" + leaver.getUsername()
            ));

            if (prize > 0) {
                walletService.processTransaction(winner, prize, "INR", "CREDIT", 
                    "Battle 1st Prize (Forfeit Win) - " + battle.getTitle() + " (#" + battle.getId() + ")", 
                    "BATTLE_PRIZE_" + battle.getId() + "_" + winner.getId() + "_" + System.currentTimeMillis());
            }

            userRepository.save(winner);

            notificationService.sendNotification(winner.getId(), "🏆 Battle Won by Forfeit", 
                "Your opponent @" + leaver.getUsername() + " left the battle! You won " + coinsAwarded + " coins (deducted from @" + leaver.getUsername() + "), ₹" + prize + " cash, and " + winXp + " XP!", 
                "fas fa-trophy", "/battles/" + battle.getId());
            notificationService.sendNotification(leaver.getId(), "Battle Forfeited", 
                "You left battle #" + battle.getId() + ". " + coinsDeducted + " coins were deducted and transferred to @" + winner.getUsername() + ".", 
                "fas fa-running", "/battles/" + battle.getId());

            // Process predictions
            List<com.example.demo.model.BattlePrediction> predictions = predictionRepository.findByBattle(battle);
            for (com.example.demo.model.BattlePrediction pred : predictions) {
                User bettor = pred.getBettor();
                if (pred.getPredictedWinner().getId().equals(winner.getId())) {
                    double payout = pred.getAmount() * 2.0;
                    bettor.addWalletBalance(payout);
                    userRepository.save(bettor);
                    notificationService.sendNotification(bettor.getId(), "Prediction Won!", "Your bet on " + winner.getUsername() + " was correct! You won " + payout + " coins.", "fas fa-coins", "/wallet");
                    try { emailService.sendBattlePredictionWinNotification(bettor, battle.getTitle(), (int) payout); } catch (Exception ignored) {}
                } else {
                    notificationService.sendNotification(bettor.getId(), "Prediction Lost", "Your bet on " + pred.getPredictedWinner().getUsername() + " was incorrect.", "fas fa-times-circle", "/wallet");
                }
            }
        }

        battleRepository.save(battle);

        TournamentBattle tb = tournamentBattleRepository.findByBattle(battle);
        if (tb != null && battle.getWinner() != null) {
            tournamentService.advanceWinner(tb.getId(), battle.getWinner().getId());
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("status", "COMPLETED");
        msg.put("reason", "PLAYER_LEFT");
        msg.put("leaverId", leaver.getId());
        msg.put("leaverUsername", leaver.getUsername());
        if (winner != null) {
            msg.put("winnerId", winner.getId());
            msg.put("winnerUsername", winner.getUsername());
        }
        messagingTemplate.convertAndSend("/topic/battle/" + battle.getId() + "/status", (Object) msg);

        auditLogService.log("BATTLE_FORFEIT", leaver.getId(), "User " + leaver.getId() + " left battle " + battle.getId() + ", winner: " + (winner != null ? winner.getId() : "none"));
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


