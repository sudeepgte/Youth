package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.model.UserActivity;
import com.example.demo.model.ActivityType;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stories")
public class StoryApiController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserStories(@PathVariable Long userId, HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        User currentUser = null;
        if (authUser instanceof User) {
            currentUser = (User) authUser;
        } else {
            Object sessionUserId = session.getAttribute("userId");
            if (sessionUserId != null) {
                try {
                    Long uid = Long.valueOf(sessionUserId.toString());
                    currentUser = userRepository.findById(uid).orElse(null);
                } catch (Exception e) {
                    // Ignore parsing error
                }
            }
        }

        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Post> activeStories = postRepository.findByUserAndPostTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                user, "STORY", LocalDateTime.now().minusHours(24));

        List<Map<String, Object>> response = new ArrayList<>();
        for (Post post : activeStories) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("mediaUrl", post.getMediaUrl());
            map.put("mediaType", post.getMediaType());
            map.put("content", post.getContent());
            map.put("createdAt", post.getCreatedAt().toString());
            map.put("username", post.getUser().getUsername());
            map.put("profilePhotoUrl", post.getUser().getProfilePhotoUrl());
            map.put("userId", post.getUser().getId());
            response.add(map);
        }

        return ResponseEntity.ok(response);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{storyId}/viewers")
    public ResponseEntity<?> getStoryViewers(@PathVariable Long storyId, HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        User currentUser = null;
        if (authUser instanceof User) {
            currentUser = (User) authUser;
        } else {
            Object sessionUserId = session.getAttribute("userId");
            if (sessionUserId != null) {
                try {
                    Long uid = Long.valueOf(sessionUserId.toString());
                    currentUser = userRepository.findById(uid).orElse(null);
                } catch (Exception e) {
                    // Ignore parsing error
                }
            }
        }

        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Post story = postRepository.findById(storyId).orElse(null);
        if (story == null) {
            return ResponseEntity.notFound().build();
        }

        // Only story owner can see viewers
        if (!story.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        List<UserActivity> views = userActivityRepository.findByPostIdAndActivityType(storyId, ActivityType.VIEW);
        
        // Return unique viewers, excluding the owner
        Set<Long> seenUserIds = new HashSet<>();
        List<Map<String, Object>> viewersList = new ArrayList<>();

        for (UserActivity act : views) {
            User viewer = act.getUser();
            if (viewer == null || viewer.getId().equals(currentUser.getId())) {
                continue;
            }
            if (seenUserIds.add(viewer.getId())) {
                Map<String, Object> viewerMap = new HashMap<>();
                viewerMap.put("id", viewer.getId());
                viewerMap.put("username", viewer.getUsername());
                viewerMap.put("profilePhotoUrl", viewer.getProfilePhotoUrl());
                viewersList.add(viewerMap);
            }
        }

        return ResponseEntity.ok(viewersList);
    }
}
