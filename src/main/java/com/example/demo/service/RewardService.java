package com.example.demo.service;

import com.example.demo.model.CoinTransaction;
import com.example.demo.model.RewardConfig;
import com.example.demo.model.User;
import com.example.demo.repository.CoinTransactionRepository;
import com.example.demo.repository.RewardConfigRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RewardService {

    @Autowired
    private RewardConfigRepository rewardConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    public RewardConfig getConfig() {
        return rewardConfigRepository.findAll().stream().findFirst().orElseGet(() -> {
            RewardConfig newConfig = new RewardConfig();
            return rewardConfigRepository.save(newConfig);
        });
    }

    public void awardDailyLogin(User user) {
        if (user.getLastLoginDate() == null || !user.getLastLoginDate().equals(LocalDate.now())) {
            RewardConfig config = getConfig();
            int amount = config.getDailyLogin();
            user.addCoins(amount);
            user.setLastLoginDate(LocalDate.now());
            userRepository.save(user);
            coinTransactionRepository.save(new CoinTransaction(user, amount, "System", "Daily Login"));
        }
    }

    public void awardVoting(User user) {
        RewardConfig config = getConfig();
        int amount = config.getVoteInEvent();
        user.addCoins(amount);
        userRepository.save(user);
        coinTransactionRepository.save(new CoinTransaction(user, amount, "Event", "Voting"));
    }

    public void awardMusicVote(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getMusicVote());
        userRepository.save(user);
    }

    public void awardRegistration(User user) {
        awardRegistration(user, null);
    }

    public synchronized void awardRegistration(User user, String ticketId) {
        if (user == null) return;
        user = userRepository.findById(user.getId()).orElse(user);

        String reason = (ticketId != null && !ticketId.isBlank()) 
                ? "Ticket Booking #" + ticketId 
                : "Event Registration";

        // Prevent duplicate reward for the same ticket/booking
        if (ticketId != null && !ticketId.isBlank()) {
            boolean alreadyAwarded = coinTransactionRepository.existsByUserAndSourceAndReason(user, "Event Booking", reason);
            if (alreadyAwarded) {
                return;
            }
        }

        RewardConfig config = getConfig();
        int amount = config.getRegisterForEvent();
        user.addCoins(amount);
        userRepository.save(user);
        coinTransactionRepository.save(new CoinTransaction(user, amount, "Event Booking", reason));
    }

    public void awardAttendance(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getAttendEvent());
        userRepository.save(user);
    }

    public void awardWinner(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getWinner());
        userRepository.save(user);
    }

    public void awardRunner(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getRunner());
        userRepository.save(user);
    }

    public void awardReferral(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getReferFriend());
        userRepository.save(user);
    }

    public void awardTalentPost(User user) {
        awardTalentPost(user, null, "POST");
    }

    public synchronized void awardTalentPost(User user, Long postId, String postType) {
        if (user == null) return;
        user = userRepository.findById(user.getId()).orElse(user);

        String type = (postType != null && postType.equalsIgnoreCase("REEL")) ? "Reel" : "Post";
        String reason = (postId != null) ? type + " #" + postId : "Created " + type;

        // Prevent duplicate rewards for the same post/reel
        if (postId != null) {
            boolean alreadyAwarded = coinTransactionRepository.existsByUserAndSourceAndReason(user, "Content Creation", reason);
            if (alreadyAwarded) {
                return;
            }
        }

        // Daily rate limit: max 5 rewarded posts/reels in 24 hours to prevent abuse
        java.time.LocalDateTime twentyFourHoursAgo = java.time.LocalDateTime.now().minusHours(24);
        long recentAwards = coinTransactionRepository.countByUserAndSourceAndTimestampAfter(user, "Content Creation", twentyFourHoursAgo);
        if (recentAwards >= 5) {
            return;
        }

        RewardConfig config = getConfig();
        int amount = config.getTalentPost();
        user.addCoins(amount);
        userRepository.save(user);
        coinTransactionRepository.save(new CoinTransaction(user, amount, "Content Creation", reason));
    }

    public void awardGamePlay(User user, String gameName) {
        RewardConfig config = getConfig();
        int amount = config.getGamePlay();
        user.addCoins(amount);
        userRepository.save(user);
        coinTransactionRepository.save(new CoinTransaction(user, amount, gameName, "Played Game"));
    }

    public void awardGameWin(User user, String gameName) {
        RewardConfig config = getConfig();
        int amount = config.getGameWin();
        user.addCoins(amount);
        userRepository.save(user);
        coinTransactionRepository.save(new CoinTransaction(user, amount, gameName, "Won Game"));
    }

    public void awardGameScore(User user, String gameName, int amount) {
        if (amount <= 0) return;
        user.addCoins(amount);
        userRepository.save(user);
        coinTransactionRepository.save(new CoinTransaction(user, amount, gameName, "Score Milestone"));
    }

    /**
     * MVP: reward coins for listening at least N seconds in a day.
     * Uses user fields to avoid extending RewardConfig/admin form.
     */
    public boolean awardDailyMusicListening(User user, int newlyRewardableSeconds) {
        if (user == null) return false;

        LocalDate today = LocalDate.now();
        if (user.getLastMusicRewardDate() != null && user.getLastMusicRewardDate().equals(today)) {
            return false;
        }

        // reset counter if it belongs to a previous day
        if (user.getMusicSecondsDate() == null || !user.getMusicSecondsDate().equals(today)) {
            user.setMusicRewardedSecondsToday(0);
            user.setMusicSecondsDate(today);
        }
        if (user.getMusicSecondsDate() == null) {
            user.setMusicRewardedSecondsToday(0);
        }

        int updatedSeconds = Math.min(60 * 60, user.getMusicRewardedSecondsToday() + Math.max(0, newlyRewardableSeconds)); // cap 1h/day
        user.setMusicRewardedSecondsToday(updatedSeconds);

        int thresholdSeconds = 10 * 60; // 10 minutes
        if (updatedSeconds >= thresholdSeconds) {
            user.addCoins(3); // keep small for MVP
            user.setLastMusicRewardDate(today);
        }

        userRepository.save(user);
        return user.getLastMusicRewardDate() != null && user.getLastMusicRewardDate().equals(today);
    }
}
