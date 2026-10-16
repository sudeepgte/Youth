package com.example.demo.service;

import com.example.demo.model.Battle;
import com.example.demo.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class VoteProtectionService {

    // Prevent DB spam for duplicate votes: BattleID -> Set of UserIDs
    private final ConcurrentHashMap<Long, Set<Long>> battleVoters = new ConcurrentHashMap<>();

    // Prevent automated voting / multiple accounts fast switching
    // UserID -> Timestamps of recent vote attempts (to detect bots)
    private final ConcurrentHashMap<Long, Deque<Long>> userVoteAttempts = new ConcurrentHashMap<>();

    // Shadowban list for users who exhibited automated voting patterns
    private final ConcurrentHashMap<Long, LocalDateTime> shadowBannedVoters = new ConcurrentHashMap<>();

    /**
     * Checks if a vote is valid and not spam/botting.
     * @return true if the vote is allowed, false if blocked.
     */
    public boolean isVoteAllowed(Long battleId, User voter, Long participantUserId) {
        Long voterId = voter.getId();

        // 1. Prevent Self-Voting
        if (voterId.equals(participantUserId)) {
            return false;
        }

        // 2. Check Shadowban
        if (shadowBannedVoters.containsKey(voterId)) {
            if (LocalDateTime.now().isBefore(shadowBannedVoters.get(voterId))) {
                return false; // Still banned
            } else {
                shadowBannedVoters.remove(voterId); // Ban expired
            }
        }

        // 3. Rate Limiting / Suspicious Pattern Detection (Max 3 attempts per 10 seconds globally)
        long now = System.currentTimeMillis();
        Deque<Long> attempts = userVoteAttempts.computeIfAbsent(voterId, k -> new ConcurrentLinkedDeque<>());
        attempts.addLast(now);

        // Remove attempts older than 10 seconds
        while (!attempts.isEmpty() && now - attempts.getFirst() > 10000) {
            attempts.removeFirst();
        }

        if (attempts.size() > 5) {
            // Suspicious voting pattern detected! Shadowban the voter for 1 hour
            shadowBannedVoters.put(voterId, LocalDateTime.now().plusHours(1));
            return false;
        }

        // 4. In-Memory Duplicate Check (prevents hitting the DB for duplicate votes)
        Set<Long> votersForBattle = battleVoters.computeIfAbsent(battleId, k -> ConcurrentHashMap.newKeySet());
        if (votersForBattle.contains(voterId)) {
            return false;
        }

        // Additional multiple-account prevention: Check if account is at least 1 hour old or has minimum XP
        // (Assuming createdAt or lastActiveAt is tracked, we can just use XP for now)
        // If a user has 0 XP and no profile picture, it might be a bot.
        // We'll allow them to vote but rate-limit them heavily if they abuse it, which is handled above.

        return true;
    }

    /**
     * Record a successful vote to prevent future duplicates in memory.
     */
    public void recordVote(Long battleId, Long voterId) {
        battleVoters.computeIfAbsent(battleId, k -> ConcurrentHashMap.newKeySet()).add(voterId);
    }
}
