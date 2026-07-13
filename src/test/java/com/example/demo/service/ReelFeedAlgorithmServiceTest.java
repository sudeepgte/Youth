package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ReelRepository;
import com.example.demo.repository.UserInterestProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.example.demo.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReelFeedAlgorithmServiceTest {

    @Mock
    private ReelRepository reelRepository;
    @Mock
    private UserInterestProfileRepository profileRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReelFeedAlgorithmService reelFeedService;

    private User creator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        creator = new User();
        creator.setId(99L);
        creator.setUsername("creator");
        
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Category preference boosts score
    // User loves FITNESS (score=10). A FITNESS reel should rank above a FOOD
    // reel with the same engagement metrics.
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testReelFeed_categoryPreferenceBoostsScore() {
        UserInterestProfile profile = new UserInterestProfile();
        profile.setFitnessScore(10.0); // user loves fitness
        profile.setFoodScore(0.0);

        Reel fitnessReel = makeReel(1L, ReelCategory.FITNESS, 5, 2, 1, 100, LocalDateTime.now());
        Reel foodReel = makeReel(2L, ReelCategory.FOOD, 5, 2, 1, 100, LocalDateTime.now());

        Page<Reel> page = new PageImpl<>(List.of(foodReel, fitnessReel)); // reversed order intentionally

        when(profileRepository.findByUserId(anyLong())).thenReturn(Optional.of(profile));
        when(reelRepository.findByIsApprovedTrue(any(Pageable.class))).thenReturn(page);

        List<Reel> result = reelFeedService.getPersonalizedFeed(1L, 10);

        assertEquals(fitnessReel.getId(), result.get(0).getId(),
                "Fitness reel should rank first for a user with high fitness preference");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Higher engagement wins within same category
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testReelFeed_higherEngagementWins() {
        UserInterestProfile profile = new UserInterestProfile(); // all scores = 0

        LocalDateTime now = LocalDateTime.now();
        Reel lowEngage = makeReel(3L, ReelCategory.GAMING, 1, 0, 0, 10, now);
        Reel highEngage = makeReel(4L, ReelCategory.GAMING, 50, 20, 5, 500, now);

        Page<Reel> page = new PageImpl<>(List.of(lowEngage, highEngage));

        when(profileRepository.findByUserId(anyLong())).thenReturn(Optional.of(profile));
        when(reelRepository.findByIsApprovedTrue(any(Pageable.class))).thenReturn(page);

        List<Reel> result = reelFeedService.getPersonalizedFeed(1L, 10);

        assertEquals(highEngage.getId(), result.get(0).getId(),
                "Higher engagement reel should rank first");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Recency decay – older reel scores lower
    // A brand-new reel vs a 72-hour-old reel with identical engagement:
    // new reel's recency multiplier ≈ e^0 = 1.0
    // old reel's recency multiplier ≈ e^(-3.6) ≈ 0.027
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testReelFeed_recencyDecay_olderReelScoresLower() {
        UserInterestProfile profile = new UserInterestProfile();

        Reel newReel = makeReel(5L, ReelCategory.TRAVEL, 10, 5, 2, 200, LocalDateTime.now());
        Reel oldReel = makeReel(6L, ReelCategory.TRAVEL, 10, 5, 2, 200, LocalDateTime.now().minusHours(72));

        Page<Reel> page = new PageImpl<>(List.of(oldReel, newReel)); // old first, should be re-ranked

        when(profileRepository.findByUserId(anyLong())).thenReturn(Optional.of(profile));
        when(reelRepository.findByIsApprovedTrue(any(Pageable.class))).thenReturn(page);

        List<Reel> result = reelFeedService.getPersonalizedFeed(1L, 10);

        assertEquals(newReel.getId(), result.get(0).getId(),
                "Newer reel should rank above older reel with identical engagement");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. New user (empty profile) → falls back to global engagement ranking
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testReelFeed_newUserGetsGlobalPopularity() {
        // profileRepository returns empty → service creates a default empty
        // UserInterestProfile
        when(profileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        LocalDateTime now = LocalDateTime.now();
        Reel niche = makeReel(7L, ReelCategory.FITNESS, 1, 0, 0, 5, now);
        Reel popular = makeReel(8L, ReelCategory.FOOD, 50, 30, 10, 1000, now);

        Page<Reel> page = new PageImpl<>(List.of(niche, popular));

        when(reelRepository.findByIsApprovedTrue(any(Pageable.class))).thenReturn(page);

        List<Reel> result = reelFeedService.getPersonalizedFeed(999L, 10);

        assertEquals(popular.getId(), result.get(0).getId(),
                "New user with no preference profile should see most globally popular reel first");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Result size never exceeds the requested limit
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testReelFeed_respectsLimit() {
        UserInterestProfile profile = new UserInterestProfile();
        when(profileRepository.findByUserId(anyLong())).thenReturn(Optional.of(profile));

        LocalDateTime now = LocalDateTime.now();
        // Supply 15 reels, ask for 5
        List<Reel> reels = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            reels.add(makeReel((long) (100 + i), ReelCategory.LIFESTYLE, i, 0, 0, i * 10, now.minusMinutes(i)));
        }

        when(reelRepository.findByIsApprovedTrue(any(Pageable.class))).thenReturn(new PageImpl<>(reels));

        List<Reel> result = reelFeedService.getPersonalizedFeed(1L, 5);

        assertTrue(result.size() <= 5, "Result must not exceed the requested limit of 5");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Reel makeReel(Long id, ReelCategory category,
            long likes, long comments, long shares, long views,
            LocalDateTime createdAt) {
        Reel r = new Reel();
        r.setId(id);
        r.setUser(creator);
        r.setCategory(category);
        r.setLikeCount(likes);
        r.setCommentCount(comments);
        r.setShareCount(shares);
        r.setViewCount(views);
        r.setVideoUrl("http://example.com/reel/" + id + ".mp4");
        r.setCreatedAt(createdAt);
        r.setApproved(true);
        return r;
    }
}
