package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.FeedAlgorithmService;
import com.example.demo.service.UserActivityService;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/feed")
public class FeedApiController {

    @Autowired
    private FeedAlgorithmService feedAlgorithmService;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    // ── 1. Personalized Feed ───────────────────────────────────────────────

    /**
     * GET /api/feed?userId=1&page=0&size=10
     * Returns a personalized ranked list of posts.
     */
    @GetMapping
    public ResponseEntity<?> getPersonalizedFeed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.badRequest().body("User not found");
        }

        List<Post> feed = feedAlgorithmService.getPersonalizedFeed(userId, page, size);
        return ResponseEntity.ok(feed);
    }

    // ── 2. Trending ────────────────────────────────────────────────────────

    /**
     * GET /api/feed/trending?limit=20
     * Returns top posts by engagement in the last 48 hours.
     */
    @RequestMapping(value = "/trending", method = RequestMethod.GET)
    public ResponseEntity<List<Post>> getTrending(
            @RequestParam(defaultValue = "20") int limit) {

        List<Post> trending = feedAlgorithmService.getTrendingPosts(limit);
        return ResponseEntity.ok(trending);
    }

    // ── 3. Recommended ────────────────────────────────────────────────────

    /**
     * GET /api/feed/recommended?userId=1&limit=10
     * Returns highly-engaging posts from outside the viewer's following list.
     */
    @RequestMapping(value = "/recommended", method = RequestMethod.GET)
    public ResponseEntity<?> getRecommended(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.badRequest().body("User not found");
        }

        List<Post> recommended = feedAlgorithmService.getRecommendedPosts(userId, limit);
        return ResponseEntity.ok(recommended);
    }

    // ── 4. Record Activity ────────────────────────────────────────────────

    /**
     * POST /api/feed/activity
     * Body: { "userId": 1, "postId": 5, "action": "SHARE", "watchTime": 0 }
     * Records a user activity (VIEW, LIKE, COMMENT, SHARE, SAVE).
     */
    @RequestMapping(value = "/activity", method = RequestMethod.POST)
    public ResponseEntity<?> recordActivity(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long postId = Long.valueOf(body.get("postId").toString());
            String action = body.get("action").toString().toUpperCase();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));

            ActivityType type = ActivityType.valueOf(action);

            if (type == ActivityType.VIEW && body.containsKey("watchTime")) {
                long watchTime = Long.parseLong(body.get("watchTime").toString());
                userActivityService.recordView(user, post, watchTime);
            } else {
                userActivityService.record(user, post, type);
            }

            return ResponseEntity.ok("Activity recorded");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
