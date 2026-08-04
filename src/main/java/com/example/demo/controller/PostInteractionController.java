package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.service.FeedAlgorithmService;

import java.util.*;

@RestController
@RequestMapping(value = "/api/posts")
public class PostInteractionController {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private PostCommentRepository postCommentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserActivityRepository userActivityRepository;
    @Autowired
    private FeedAlgorithmService feedAlgorithmService;

    private User getUserFromSession(HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            return (User) sessionUser;
        }
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = null;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }
                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    // ── Like / Unlike (toggle) ──────────────────────────────────────────────
    @Transactional
    @RequestMapping(value = "/{postId}/like", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long postId, HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();
        user = userRepository.findById(user.getId()).orElse(user);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);
        boolean liked;
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            postLikeRepository.flush();
            liked = false;
        } else {
            postLikeRepository.saveAndFlush(new PostLike(post, user));
            liked = true;

            // Trigger Notification for the owner (if not liking own post)
            if (!post.getUser().getId().equals(user.getId())) {
                String typeStr = post.getPostType() != null ? post.getPostType().toLowerCase() : "post";
                String postSnippet = post.getContent() != null && !post.getContent().isBlank() ? 
                    (post.getContent().length() > 15 ? post.getContent().substring(0, 15) + "..." : post.getContent()) : "media";
                String msg = "@" + user.getUsername() + " liked your " + typeStr + " (" + postSnippet + ")";
                notificationRepository.save(new Notification(post.getUser(), user, msg, "LIKE", post.getId()));
            }
        }

        long count = postLikeRepository.countByPost(post);
        feedAlgorithmService.evictFeedCache();
        Map<String, Object> resp = new HashMap<>();
        resp.put("liked", liked);
        resp.put("likeCount", count);
        return ResponseEntity.ok(resp);
    }

    // ── Add Comment ─────────────────────────────────────────────────────────
    @RequestMapping(value = "/{postId}/comment", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long postId,
            @RequestParam String content,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();
        user = userRepository.findById(user.getId()).orElse(user);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        if (content == null || content.trim().isEmpty())
            return ResponseEntity.badRequest().build();

        PostComment comment = postCommentRepository.save(new PostComment(post, user, content.trim()));

        // Trigger Notification for the owner (if not commenting on own post)
        if (!post.getUser().getId().equals(user.getId())) {
            String typeStr = post.getPostType() != null ? post.getPostType().toLowerCase() : "post";
            String postSnippet = post.getContent() != null && !post.getContent().isBlank() ? 
                (post.getContent().length() > 15 ? post.getContent().substring(0, 15) + "..." : post.getContent()) : "media";
            String msg = "@" + user.getUsername() + " commented on (" + postSnippet + "): \"" + 
                         (content.length() > 20 ? content.substring(0, 17) + "..." : content.trim()) + "\"";
            notificationRepository.save(new Notification(post.getUser(), user, msg, "COMMENT", post.getId()));
        }

        feedAlgorithmService.evictFeedCache();
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", comment.getId());
        resp.put("username", user.getUsername());
        resp.put("photo", user.getProfilePhotoUrl() != null ? user.getProfilePhotoUrl() : "");
        resp.put("content", comment.getContent());
        resp.put("date", comment.getCreatedAt().toString());
        resp.put("count", postCommentRepository.countByPost(post));
        return ResponseEntity.ok(resp);
    }

    // ── Get Comments ────────────────────────────────────────────────────────
    @RequestMapping(value = "/{postId}/comments", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getComments(
            @PathVariable Long postId, HttpSession session) {

        if (getUserFromSession(session) == null)
            return ResponseEntity.status(401).build();
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        List<Map<String, Object>> result = new ArrayList<>();
        for (PostComment c : postCommentRepository.findByPostOrderByCreatedAtAsc(post)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("username", c.getUser().getUsername());
            m.put("photo", c.getUser().getProfilePhotoUrl() != null ? c.getUser().getProfilePhotoUrl() : "");
            m.put("content", c.getContent());
            m.put("date", c.getCreatedAt().toString());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ── Get post stats (likes + comments count + did current user like/save?) ──
    @RequestMapping(value = "/{postId}/stats", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getStats(
            @PathVariable Long postId, HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();
        user = userRepository.findById(user.getId()).orElse(user);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        boolean saved = userActivityRepository
            .findByUserIdAndPostId(user.getId(), postId)
            .stream().anyMatch(a -> a.getActivityType() == ActivityType.SAVE);

        Map<String, Object> resp = new HashMap<>();
        resp.put("likes", postLikeRepository.countByPost(post));
        resp.put("comments", postCommentRepository.countByPost(post));
        resp.put("liked", postLikeRepository.existsByPostAndUser(post, user));
        resp.put("saved", saved);
        return ResponseEntity.ok(resp);
    }

    // ── Save / Unsave toggle (bookmark) ─────────────────────────────────────
    @Transactional
    @RequestMapping(value = "/{postId}/save", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> toggleSave(
            @PathVariable Long postId, HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();
        user = userRepository.findById(user.getId()).orElse(user);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        List<UserActivity> existing = userActivityRepository.findByUserIdAndPostId(user.getId(), postId);
        Optional<UserActivity> savedAct = existing.stream()
            .filter(a -> a.getActivityType() == ActivityType.SAVE).findFirst();

        boolean nowSaved;
        if (savedAct.isPresent()) {
            userActivityRepository.delete(savedAct.get());
            nowSaved = false;
        } else {
            userActivityRepository.save(new UserActivity(user, post, ActivityType.SAVE));
            nowSaved = true;
        }

        feedAlgorithmService.evictFeedCache();
        Map<String, Object> resp = new HashMap<>();
        resp.put("saved", nowSaved);
        return ResponseEntity.ok(resp);
    }

    // ── Edit post caption / hashtags (owner only) ────────────────────────────
    @Transactional
    @RequestMapping(value = "/{postId}/edit", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> editPost(
            @PathVariable Long postId,
            @RequestParam String content,
            @RequestParam(required = false) String hashtags,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();
        if (!post.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403).build();

        post.setContent(content.trim());
        if (hashtags != null) post.setHashtags(hashtags.trim());
        postRepository.save(post);

        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        resp.put("content", post.getContent());
        resp.put("hashtags", post.getHashtags());
        return ResponseEntity.ok(resp);
    }
}
