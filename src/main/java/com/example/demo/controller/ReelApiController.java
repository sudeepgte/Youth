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

    // 1. Upload Reel
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<?> uploadReel(
            @RequestParam("userId") Long userId,
            @RequestParam("video") MultipartFile video,
            @ModelAttribute Reel reelMetadata) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Reel savedReel = reelService.uploadReel(user, video, reelMetadata);
            return ResponseEntity.ok(savedReel);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    // 2. Fetch Personalized Feed
    @RequestMapping(value = "/feed", method = RequestMethod.GET)
    public ResponseEntity<List<Reel>> getReelsFeed(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<Reel> feed = reelFeedAlgorithmService.getPersonalizedFeed(userId, limit);
        return ResponseEntity.ok(feed);
    }

    // 3. User Interaction Tracking
    @RequestMapping(value = "/{id}/interact", method = RequestMethod.POST)
    public ResponseEntity<?> recordInteraction(
            @PathVariable Long id,
            @RequestParam("userId") Long userId,
            @RequestBody Map<String, Object> interactionData) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String action = (String) interactionData.get("action"); // like, comment, share, save, complete, watch
            Integer watchTime = (Integer) interactionData.get("watchTime");

            reelInteractionService.recordInteraction(user, id, action, watchTime);
            return ResponseEntity.ok("Interaction recorded successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Interaction recording failed: " + e.getMessage());
        }
    }
}
