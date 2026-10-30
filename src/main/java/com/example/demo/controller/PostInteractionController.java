package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.service.FeedAlgorithmService;
import com.example.demo.config.JwtUtil;

import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private com.example.demo.service.EmailService emailService;
    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;
    @Autowired(required = false)
    private JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User resolveUser(HttpSession session, HttpServletRequest req) {
        HttpServletRequest request = (req != null) ? req : this.httpServletRequest;
        if (request != null) {
            Object authUser = request.getAttribute("authenticatedUser");
            if (authUser instanceof User) {
                return (User) authUser;
            }
            if ("admin".equals(authUser)) {
                User admin = userRepository.findByUsername("admin");
                if (admin != null) return admin;
            }
        }
        if (session != null) {
            Object sessionUser = session.getAttribute("user");
            if (sessionUser instanceof User) {
                return (User) sessionUser;
            }
            if (sessionUser instanceof String) {
                User u = userRepository.findByUsername((String) sessionUser);
                if (u != null) return u;
            }
            if ("admin".equals(session.getAttribute("admin")) || "ADMIN".equals(session.getAttribute("role"))) {
                User admin = userRepository.findByUsername("admin");
                if (admin != null) return admin;
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
                        User u = userRepository.findById(userId).orElse(null);
                        if (u != null) return u;
                    }
                } catch (Exception ignored) {}
            }
        }
        if (request != null && jwtUtil != null) {
            try {
                String token = null;
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
                if (token == null && request.getCookies() != null) {
                    for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                        if ("jwtToken".equals(c.getName())) {
                            token = c.getValue();
                            break;
                        }
                    }
                }
                if (token != null) {
                    String username = jwtUtil.extractUsername(token);
                    if (username != null) {
                        return userRepository.findByUsername(username);
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private User getUserFromSession(HttpSession session) {
        return resolveUser(session, null);
    }

    // ── Like / Unlike (toggle) ──────────────────────────────────────────────
    @Transactional
    @RequestMapping(value = "/{postId}/like", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long postId,
            HttpServletRequest request,
            HttpSession session) {

        User user = resolveUser(session, request);
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
            if (post.getUser() != null && user.getId() != null && !post.getUser().getId().equals(user.getId())) {
                try {
                    String typeStr = post.getPostType() != null ? post.getPostType().toLowerCase() : "post";
                    String postSnippet = post.getContent() != null && !post.getContent().isBlank() ? 
                        (post.getContent().length() > 15 ? post.getContent().substring(0, 15) + "..." : post.getContent()) : "media";
                    String msg = "@" + user.getUsername() + " liked your " + typeStr + " (" + postSnippet + ")";
                    notificationRepository.save(new Notification(post.getUser(), user, msg, "LIKE", post.getId()));
                } catch (Exception ignored) {}
            }
        }

        long count = postLikeRepository.countByPost(post);
        feedAlgorithmService.evictFeedCache();
        Map<String, Object> resp = new HashMap<>();
        resp.put("liked", liked);
        resp.put("likeCount", count);
        return ResponseEntity.ok(resp);
    }

    // ── Add Comment / Reply ──────────────────────────────────────────────────
    @Transactional
    @RequestMapping(value = "/{postId}/comment", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long postId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Long parentId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request,
            HttpSession session) {

        User user = resolveUser(session, request);
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Unauthorized. Please log in to comment.");
            return ResponseEntity.status(401).body(err);
        }
        user = userRepository.findById(user.getId()).orElse(user);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Post not found");
            return ResponseEntity.status(404).body(err);
        }

        if (post.isCommentsDisabled()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Comments are disabled for this post");
            return ResponseEntity.badRequest().body(err);
        }

        String commentText = content;
        Long targetParentId = parentId;

        if (body != null) {
            if ((commentText == null || commentText.trim().isEmpty()) && body.containsKey("content") && body.get("content") != null) {
                commentText = Objects.toString(body.get("content"), null);
            }
            if (targetParentId == null && body.containsKey("parentId") && body.get("parentId") != null) {
                try {
                    String pStr = body.get("parentId").toString().trim();
                    if (!pStr.isEmpty() && !"null".equalsIgnoreCase(pStr)) {
                        targetParentId = Long.valueOf(pStr);
                    }
                } catch (Exception ignored) {}
            }
        }

        if ((commentText == null || commentText.trim().isEmpty()) && request != null) {
            if (request.getParameter("content") != null) {
                commentText = request.getParameter("content");
            }
            if (targetParentId == null && request.getParameter("parentId") != null) {
                try {
                    String pStr = request.getParameter("parentId").trim();
                    if (!pStr.isEmpty() && !"null".equalsIgnoreCase(pStr)) {
                        targetParentId = Long.valueOf(pStr);
                    }
                } catch (Exception ignored) {}
            }
        }

        if (commentText == null || commentText.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Comment content cannot be empty");
            return ResponseEntity.badRequest().body(err);
        }

        PostComment parentComment = null;
        if (targetParentId != null && targetParentId > 0) {
            parentComment = postCommentRepository.findById(targetParentId).orElse(null);
            if (parentComment != null && parentComment.getParentComment() != null) {
                parentComment = parentComment.getParentComment();
            }
        }

        PostComment comment = new PostComment(post, user, commentText.trim(), parentComment);
        comment = postCommentRepository.save(comment);

        try {
            if (parentComment != null) {
                if (parentComment.getReplies() == null) {
                    parentComment.setReplies(new ArrayList<>());
                }
                parentComment.getReplies().add(comment);
                postCommentRepository.save(parentComment);
            }
            if (post.getComments() == null) {
                post.setComments(new ArrayList<>());
            }
            post.getComments().add(comment);
            postRepository.save(post);
        } catch (Exception ignored) {}

        try {
            userActivityRepository.save(new UserActivity(user, post, ActivityType.COMMENT));
        } catch (Exception ignored) {}

        try {
            if (parentComment != null) {
                User parentAuthor = parentComment.getUser();
                if (parentAuthor != null && user.getId() != null && !parentAuthor.getId().equals(user.getId())) {
                    String parentSnippet = parentComment.getContent() != null && !parentComment.getContent().isBlank() ? 
                        (parentComment.getContent().length() > 20 ? parentComment.getContent().substring(0, 17) + "..." : parentComment.getContent()) : "comment";
                    String msg = "@" + user.getUsername() + " replied to your comment (" + parentSnippet + "): \"" + 
                                 (commentText.length() > 20 ? commentText.substring(0, 17) + "..." : commentText.trim()) + "\"";
                    notificationRepository.save(new Notification(parentAuthor, user, msg, "COMMENT_REPLY", post.getId()));
                    try { emailService.sendPostCommentNotification(parentAuthor, user, (post.getContent() != null ? post.getContent() : "post"), commentText.trim()); } catch (Exception ignored) {}
                }
            } else {
                User postAuthor = post.getUser();
                if (postAuthor != null && user.getId() != null && !postAuthor.getId().equals(user.getId())) {
                    String typeStr = post.getPostType() != null ? post.getPostType().toLowerCase() : "post";
                    String postSnippet = post.getContent() != null && !post.getContent().isBlank() ? 
                        (post.getContent().length() > 15 ? post.getContent().substring(0, 15) + "..." : post.getContent()) : "media";
                    String msg = "@" + user.getUsername() + " commented on (" + postSnippet + "): \"" + 
                                 (commentText.length() > 20 ? commentText.substring(0, 17) + "..." : commentText.trim()) + "\"";
                    notificationRepository.save(new Notification(postAuthor, user, msg, "COMMENT", post.getId()));
                    try { emailService.sendPostCommentNotification(postAuthor, user, (post.getContent() != null ? post.getContent() : "post"), commentText.trim()); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        feedAlgorithmService.evictFeedCache();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", comment.getId());
        resp.put("username", user.getUsername());
        resp.put("photo", user.getProfilePhotoUrl() != null ? user.getProfilePhotoUrl() : "");
        resp.put("content", comment.getContent());
        resp.put("date", comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : java.time.LocalDateTime.now().toString());
        resp.put("parentId", parentComment != null ? parentComment.getId() : null);
        resp.put("parentUsername", (parentComment != null && parentComment.getUser() != null) ? parentComment.getUser().getUsername() : null);
        resp.put("count", postCommentRepository.countByPost(post));
        return ResponseEntity.ok(resp);
    }

    // ── Get Comments ────────────────────────────────────────────────────────
    @RequestMapping(value = "/{postId}/comments", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getComments(
            @PathVariable Long postId,
            HttpServletRequest request,
            HttpSession session) {

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        List<Map<String, Object>> result = new ArrayList<>();
        List<PostComment> comments = postCommentRepository.findByPostOrderByCreatedAtAsc(post);
        for (PostComment c : comments) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("username", c.getUser() != null ? c.getUser().getUsername() : "Unknown");
            m.put("photo", (c.getUser() != null && c.getUser().getProfilePhotoUrl() != null) ? c.getUser().getProfilePhotoUrl() : "");
            m.put("content", c.getContent());
            m.put("date", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            m.put("parentId", c.getParentComment() != null ? c.getParentComment().getId() : null);
            m.put("parentUsername", (c.getParentComment() != null && c.getParentComment().getUser() != null) ? c.getParentComment().getUser().getUsername() : null);
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
        List<UserActivity> savedActs = existing.stream()
            .filter(a -> a.getActivityType() == ActivityType.SAVE)
            .collect(Collectors.toList());

        boolean nowSaved;
        if (!savedActs.isEmpty()) {
            // Delete all existing save records to prevent duplicates
            userActivityRepository.deleteAll(savedActs);
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
