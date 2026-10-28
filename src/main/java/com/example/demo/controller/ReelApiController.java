package com.example.demo.controller;

import com.example.demo.model.Reel;
import com.example.demo.model.User;
import com.example.demo.service.ReelFeedAlgorithmService;
import com.example.demo.service.ReelInteractionService;
import com.example.demo.service.ReelService;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/reels")
public class ReelApiController {

    @Autowired
    private ReelService reelService;

    @Autowired
    private ReelInteractionService reelInteractionService;

    @Autowired
    private ReelFeedAlgorithmService reelFeedAlgorithmService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.demo.config.JwtUtil jwtUtil;

    private User getAuthenticatedUser(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpSession session) {
        Object authUser = request.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return (User) authUser;
        }
        if (authUser instanceof String) {
            return userRepository.findByUsername((String) authUser);
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String username = jwtUtil.extractUsername(authHeader.substring(7));
                if (username != null) {
                    return userRepository.findByUsername(username);
                }
            } catch (Exception e) {}
        }
        if (session != null) {
            Object sessionUser = session.getAttribute("user");
            if (sessionUser instanceof User) return (User) sessionUser;
            if (sessionUser instanceof String) return userRepository.findByUsername((String) sessionUser);
            Object userIdObj = session.getAttribute("userId");
            if (userIdObj instanceof Number) {
                return userRepository.findById(((Number) userIdObj).longValue()).orElse(null);
            }
        }
        return null;
    }

    // 1. Upload Reel
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<?> uploadReel(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam("video") MultipartFile video,
            @ModelAttribute Reel reelMetadata,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpSession session) {
        try {
            User authUser = getAuthenticatedUser(request, session);
            if (authUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized: Valid session or Bearer token required"));
            }

            // IDOR Protection: Ignore caller-provided userId or reject if attempting to impersonate another user (DEF-07)
            if (userId != null && !userId.equals(authUser.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Cannot upload reel on behalf of another user"));
            }

            Reel savedReel = reelService.uploadReel(authUser, video, reelMetadata);
            return ResponseEntity.ok(savedReel);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    // 2. Fetch Personalized Feed
    @RequestMapping(value = "/feed", method = RequestMethod.GET)
    public ResponseEntity<List<Reel>> getReelsFeed(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpSession session) {
        User authUser = getAuthenticatedUser(request, session);
        Long targetUserId = authUser != null ? authUser.getId() : (userId != null ? userId : 0L);
        List<Reel> feed = reelFeedAlgorithmService.getPersonalizedFeed(targetUserId, limit);
        return ResponseEntity.ok(feed);
    }

    // 3. User Interaction Tracking
    @RequestMapping(value = "/{id}/interact", method = RequestMethod.POST)
    public ResponseEntity<?> recordInteraction(
            @PathVariable Long id,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestBody Map<String, Object> interactionData,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpSession session) {
        try {
            User authUser = getAuthenticatedUser(request, session);
            if (authUser == null && userId != null) {
                authUser = userRepository.findById(userId).orElse(null);
            }
            if (authUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            String action = (String) interactionData.get("action"); // like, comment, share, save, complete, watch
            Integer watchTime = (Integer) interactionData.get("watchTime");

            reelInteractionService.recordInteraction(authUser, id, action, watchTime);
            return ResponseEntity.ok("Interaction recorded successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Interaction recording failed: " + e.getMessage());
        }
    }
}

