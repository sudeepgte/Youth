package com.example.demo.service;

import com.example.demo.model.Reel;
import com.example.demo.model.UserInterestProfile;
import com.example.demo.repository.ReelRepository;
import com.example.demo.repository.UserInterestProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReelFeedAlgorithmService {

    @Autowired
    private ReelRepository reelRepository;

    @Autowired
    private UserInterestProfileRepository profileRepository;

    @Autowired
    private com.example.demo.repository.UserRepository userRepository;

    // Configurable Algorithm Weights
    private static final double WEIGHT_LIKE = 1.5;
    private static final double WEIGHT_COMMENT = 2.0;
    private static final double WEIGHT_SHARE = 3.0;
    private static final double WEIGHT_VIEW = 0.5;
    private static final double WEIGHT_COMPLETION = 2.5;
    private static final double WEIGHT_RECENCY = 50.0; // Decay factor multiplier
    private static final double WEIGHT_RELATIONSHIP = 10.0; // Bonus for followed creators

    public List<Reel> getPersonalizedFeed(Long userId, int limit) {
        // 1. Fetch user interest profile (default to 0s if new user)
        UserInterestProfile profile = profileRepository.findByUserId(userId)
                .orElse(new UserInterestProfile());

        // 2. Fetch a pool of candidate reels (e.g., recent 100 approved reels to score)
        Page<Reel> candidateReels = reelRepository.findByIsApprovedTrue(
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")));

        // Fetch IDs of users the viewer follows
        java.util.Set<Long> followingIds = userRepository.findById(userId)
                .map(u -> u.getFollowing().stream().map(com.example.demo.model.User::getId).collect(Collectors.toSet()))
                .orElse(java.util.Collections.emptySet());

        // 3. Score and sort candidates
        List<Reel> rankedReels = candidateReels.getContent().stream()
                .filter(reel -> !reel.getUser().isPrivateAccount() || followingIds.contains(reel.getUser().getId()) || reel.getUser().getId().equals(userId))
                .sorted(Comparator.comparingDouble((Reel reel) -> calculateScore(reel, profile, userId)).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        return rankedReels;
    }

    private double calculateScore(Reel reel, UserInterestProfile profile, Long currentUserId) {
        double score = 0.0;

        // --- 1. User Interest Category Score ---
        double categoryScore = 0.0;
        switch (reel.getCategory()) {
            case FITNESS -> categoryScore = profile.getFitnessScore();
            case FOOD -> categoryScore = profile.getFoodScore();
            case TRAVEL -> categoryScore = profile.getTravelScore();
            case GAMING -> categoryScore = profile.getGamingScore();
            case FASHION -> categoryScore = profile.getFashionScore();
            case LIFESTYLE -> categoryScore = profile.getLifestyleScore();
        }

        // --- 2. Global Popularity / Engagement ---
        double engagementScore = (reel.getLikeCount() * WEIGHT_LIKE) +
                (reel.getCommentCount() * WEIGHT_COMMENT) +
                (reel.getShareCount() * WEIGHT_SHARE) +
                (reel.getViewCount() * WEIGHT_VIEW);

        // Simulated completion rate bonus (if reel has high view-to-completion ratio)
        // In a real app, this would be computed from UserReelInteraction aggregates.
        double pseudoCompletionRate = reel.getViewCount() > 0 ? (double) reel.getLikeCount() / reel.getViewCount() : 0;
        double completionScore = pseudoCompletionRate * WEIGHT_COMPLETION;

        // --- 3. Recency Decay ---
        long hoursOld = ChronoUnit.HOURS.between(reel.getCreatedAt(), LocalDateTime.now());
        // Exponential decay: e^(-0.05 * hours).
        // Newer reels retain high multiplier (near 1.0), older ones drop off.
        double recencyMultiplier = Math.exp(-0.05 * hoursOld);

        // --- 4. Relationship Bonus ---
        double relationshipScore = 0.0;
        // Check if currentUserId follows reel.getUser().getId()
        // if (userService.isFollowing(currentUserId, reel.getUser().getId())) {
        // relationshipScore = WEIGHT_RELATIONSHIP;
        // }

        // --- FINAL CALCULATION ---
        score = (categoryScore * 2.0) + (engagementScore * recencyMultiplier) + completionScore + relationshipScore;

        // DEBUG LOGGING
        System.out.println("--- Scoring Reel ID: " + reel.getId() + " Category: " + reel.getCategory() + " ---");
        System.out.println("Category Preference: " + categoryScore);
        System.out.println("Raw Engagement: " + engagementScore);
        System.out.println("Recency Decay Multiplier: " + recencyMultiplier);
        System.out.println("Final Score: " + score);
        System.out.println("-----------------------------------");

        return score;
    }
}
