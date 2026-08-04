package com.example.demo.controller;

import com.example.demo.config.ActiveLoginRegistry;
import com.example.demo.config.JwtUtil;
import com.example.demo.config.TokenBlacklist;
import com.example.demo.model.*;
import com.example.demo.music.Track;
import com.example.demo.music.TrackRepository;
import com.example.demo.music.TrackStatus;
import com.example.demo.music.room.MusicRoom;
import com.example.demo.music.room.MusicRoomRepository;
import com.example.demo.repository.*;
import com.example.demo.service.FeedAlgorithmService;
import com.example.demo.service.RewardService;
import com.example.demo.service.SecretRewardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON API for the Flutter mobile app (non-admin features).
 */
@RestController
@RequestMapping("/api/mobile")
public class MobileApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRegistrationRepository eventRegistrationRepository;
    @Autowired private BattleRepository battleRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private TrackRepository trackRepository;
    @Autowired private MusicRoomRepository musicRoomRepository;
    @Autowired private FollowRequestRepository followRequestRepository;
    @Autowired private PostCollaborationRepository postCollaborationRepository;
    @Autowired private UserRewardRepository userRewardRepository;
    @Autowired private SecretRewardService secretRewardService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private TokenBlacklist tokenBlacklist;
    @Autowired private ActiveLoginRegistry activeLoginRegistry;
    @Autowired private RewardService rewardService;
    @Autowired private FeedAlgorithmService feedAlgorithmService;
    @Autowired private HttpServletRequest httpServletRequest;

    private User currentUser(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return userRepository.findById(((User) authUser).getId()).orElse(null);
        }
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            return userRepository.findById(((User) sessionUser).getId()).orElse(null);
        }
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long id = userIdObj instanceof Number
                        ? ((Number) userIdObj).longValue()
                        : Long.parseLong(userIdObj.toString());
                return userRepository.findById(id).orElse(null);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Map<String, Object> userDto(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("email", u.getEmail());
        m.put("dob", u.getDob() != null ? u.getDob().toString() : null);
        m.put("gender", u.getGender());
        m.put("bio", u.getBio());
        m.put("aboutMe", u.getAboutMe());
        m.put("skills", u.getSkills());
        m.put("collegeName", u.getCollegeName());
        m.put("profilePhotoUrl", u.getProfilePhotoUrl() != null ? u.getProfilePhotoUrl() : u.getProfilePicture());
        m.put("privateAccount", u.isPrivateAccount());
        m.put("xp", u.getXp());
        m.put("level", u.getLevel());
        m.put("coins", u.getCoins() != null ? u.getCoins() : 0);
        m.put("walletBalance", u.getWalletBalance() != null ? u.getWalletBalance() : 0.0);
        m.put("followersCount", u.getFollowers() != null ? u.getFollowers().size() : 0);
        m.put("followingCount", u.getFollowing() != null ? u.getFollowing().size() : 0);
        m.put("isPremium", u.isPremium());
        m.put("hasDiscount", u.isHasDiscount());
        m.put("hasFreeEntry", u.isHasFreeEntry());
        m.put("profileBoostUntil", u.getProfileBoostUntil() != null ? u.getProfileBoostUntil().toString() : null);
        m.put("status", u.getStatus());
        return m;
    }

    private Map<String, Object> postDto(Post p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("content", p.getContent());
        m.put("mediaUrl", p.getMediaUrl());
        m.put("mediaType", p.getMediaType());
        m.put("hashtags", p.getHashtags());
        m.put("postType", p.getPostType());
        m.put("category", p.getCategory());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("likeCount", p.getLikes() != null ? p.getLikes().size() : 0);
        m.put("commentCount", p.getComments() != null ? p.getComments().size() : 0);
        m.put("commentsDisabled", p.isCommentsDisabled());
        if (p.getUser() != null) {
            Map<String, Object> author = new LinkedHashMap<>();
            author.put("id", p.getUser().getId());
            author.put("username", p.getUser().getUsername());
            author.put("profilePhotoUrl", p.getUser().getProfilePhotoUrl() != null
                    ? p.getUser().getProfilePhotoUrl() : p.getUser().getProfilePicture());
            author.put("level", p.getUser().getLevel());
            m.put("user", author);
        }
        return m;
    }

    private Map<String, Object> followRequestDto(FollowRequest fr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", fr.getId());
        m.put("createdAt", fr.getCreatedAt() != null ? fr.getCreatedAt().toString() : null);
        if (fr.getSender() != null) {
            m.put("senderId", fr.getSender().getId());
            m.put("senderUsername", fr.getSender().getUsername());
            m.put("senderPhoto", fr.getSender().getProfilePhotoUrl());
        }
        return m;
    }

    private Map<String, Object> collaborationDto(PostCollaboration c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("status", c.getStatus() != null ? c.getStatus().name() : null);
        if (c.getPost() != null) {
            m.put("postId", c.getPost().getId());
            m.put("postContent", c.getPost().getContent());
            if (c.getPost().getUser() != null) {
                m.put("fromUserId", c.getPost().getUser().getId());
                m.put("fromUsername", c.getPost().getUser().getUsername());
            }
        }
        return m;
    }

    private Map<String, Object> rewardDto(UserReward reward) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", reward.getId());
        m.put("rewardCode", reward.getRewardCode());
        m.put("status", reward.getStatus());
        m.put("issueDate", reward.getIssueDate() != null ? reward.getIssueDate().toString() : null);
        m.put("expiryDate", reward.getExpiryDate() != null ? reward.getExpiryDate().toString() : null);
        if (reward.getEvent() != null) {
            m.put("eventId", reward.getEvent().getId());
            m.put("eventTitle", reward.getEvent().getTitle());
        }
        if (reward.getSecretReward() != null) {
            m.put("partnerName", reward.getSecretReward().getBusinessName());
            m.put("offerTitle", reward.getSecretReward().getRewardName());
            m.put("offerDescription", reward.getSecretReward().getDescription());
        }
        return m;
    }

    private Map<String, Object> eventDto(Event e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("title", e.getTitle());
        m.put("description", e.getDescription());
        m.put("imageUrl", e.getImageUrl());
        m.put("dateTime", e.getDateTime() != null ? e.getDateTime().toString() : null);
        m.put("venue", e.getVenue());
        m.put("price", e.getPrice());
        m.put("category", e.getCategory());
        m.put("organizer", e.getOrganizer());
        m.put("status", e.getStatus());
        m.put("maxParticipants", e.getMaxParticipants());
        m.put("entryFeeType", e.getEntryFeeType());
        m.put("eventMode", e.getEventMode());
        m.put("meetingLink", e.getMeetingLink());
        m.put("pollVotes", e.getPollVotes());
        m.put("votingStatus", e.getVotingStatus());
        m.put("votingEndDate", e.getVotingEndDate() != null ? e.getVotingEndDate().toString() : null);
        m.put("latitude", e.getLatitude());
        m.put("longitude", e.getLongitude());
        m.put("vipPrice", e.getVipPrice());
        m.put("regularPrice", e.getRegularPrice());
        m.put("enableSecretRewards", e.isEnableSecretRewards());
        return m;
    }

    private Map<String, Object> battleDto(Battle b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("title", b.getTitle());
        m.put("category", b.getCategory());
        m.put("roomCode", b.getRoomCode());
        m.put("status", b.getStatus());
        m.put("maxParticipants", b.getMaxParticipants());
        m.put("durationHours", b.getDurationHours());
        m.put("mode", b.getMode());
        m.put("venue", b.getVenue());
        m.put("eventDate", b.getEventDate());
        m.put("eventTime", b.getEventTime());
        m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        m.put("startedAt", b.getStartedAt() != null ? b.getStartedAt().toString() : null);
        m.put("endsAt", b.getEndsAt() != null ? b.getEndsAt().toString() : null);
        m.put("participantCount", b.getParticipants() != null ? b.getParticipants().size() : 0);
        if (b.getCreator() != null) {
            m.put("creatorUsername", b.getCreator().getUsername());
            m.put("creatorId", b.getCreator().getId());
        }
        if (b.getWinner() != null) {
            m.put("winnerUsername", b.getWinner().getUsername());
        }
        return m;
    }

    // ── Auth ──────────────────────────────────────────────────────────────

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password required"));
        }
        if ("admin".equals(username)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin login is not available on mobile"));
        }
        User user = userRepository.findByUsername(username);
        if (user == null || !password.equals(user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
        if ("BANNED".equals(user.getStatus()) || "SUSPENDED".equals(user.getStatus())) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is " + user.getStatus().toLowerCase()));
        }
        if (activeLoginRegistry.isUserAlreadyLoggedIn(username, null)) {
            return ResponseEntity.status(409).body(Map.of("error", "Already logged in on another device"));
        }
        rewardService.awardDailyLogin(user);
        user = userRepository.findById(user.getId()).orElse(user);
        String token = jwtUtil.generateToken(username);
        activeLoginRegistry.registerLogin(username, token);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("user", userDto(user));
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String gender = body.get("gender");
        String dobStr = body.get("dob");
        if (username == null || email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username, email and password required"));
        }
        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));
        }
        if (userRepository.findByEmail(email) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        if (password.length() < 8 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*") || !password.matches(".*[@$!%*?&].*")) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Password must be 8+ chars with upper, lower, digit and special (@$!%*?&)"));
        }
        if (!email.toLowerCase().endsWith(".com")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email must end with .com"));
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setGender(gender);
        if (dobStr != null && !dobStr.isBlank()) {
            try {
                LocalDate dob = LocalDate.parse(dobStr);
                if (dob.isAfter(LocalDate.now())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Date of birth cannot be in the future"));
                }
                user.setDob(dob);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid date of birth (use YYYY-MM-DD)"));
            }
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Registered successfully", "user", userDto(user)));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");
        if (username == null || email == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields required"));
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }
        if (newPassword.length() < 8 || !newPassword.matches(".*[A-Z].*") || !newPassword.matches(".*[a-z].*")
                || !newPassword.matches(".*\\d.*") || !newPassword.matches(".*[@$!%*?&].*")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Weak password"));
        }
        User user = userRepository.findByUsername(username);
        if (user == null || !email.equalsIgnoreCase(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        user.setPassword(newPassword);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpSession session) {
        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token != null) {
            tokenBlacklist.blacklist(token);
            try {
                activeLoginRegistry.removeLogin(jwtUtil.extractUsername(token));
            } catch (Exception ignored) {}
        }
        try { session.invalidate(); } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        return ResponseEntity.ok(userDto(user));
    }

    // ── Feed / Posts ──────────────────────────────────────────────────────

    @GetMapping("/feed")
    @Transactional(readOnly = true)
    public ResponseEntity<?> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Post> feed = feedAlgorithmService.getPersonalizedFeed(user.getId(), page, size);
        return ResponseEntity.ok(feed.stream().map(this::postDto).collect(Collectors.toList()));
    }

    @Transactional
    @PostMapping(value = "/posts", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createPost(
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String hashtags,
            @RequestParam(required = false, defaultValue = "POST") String postType,
            @RequestParam(required = false) String category,
            HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String mediaUrl = null;
        String mediaType = null;
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path uploadPath = Paths.get("src/main/resources/static/uploads/");
                Files.createDirectories(uploadPath);
                Path targetPath = Paths.get("target/classes/static/uploads/");
                Files.createDirectories(targetPath);
                Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.copy(file.getInputStream(), targetPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                    Files.copy(uploadPath.resolve(fileName), targetPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }
                mediaUrl = "/uploads/" + fileName;
                String ct = file.getContentType();
                mediaType = (ct != null && ct.startsWith("video")) ? "VIDEO" : "IMAGE";
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Upload failed: " + e.getMessage()));
            }
        }

        Post post = new Post(content, user, mediaUrl, mediaType, hashtags, postType, category);
        feedAlgorithmService.savePost(post);
        return ResponseEntity.ok(postDto(post));
    }

    // ── Profile ───────────────────────────────────────────────────────────

    @GetMapping("/profile/{username}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> profile(@PathVariable String username, HttpSession session) {
        User current = currentUser(session);
        if (current == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User target = userRepository.findByUsername(username);
        if (target == null) return ResponseEntity.notFound().build();
        if ("BANNED".equals(target.getStatus()) || "SUSPENDED".equals(target.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "User blocked"));
        }
        boolean isOwn = current.getId().equals(target.getId());
        boolean following = current.getFollowing() != null && current.getFollowing().contains(target);
        boolean privateBlocked = target.isPrivateAccount() && !isOwn && !following;

        Map<String, Object> resp = userDto(target);
        resp.put("isOwnProfile", isOwn);
        resp.put("isFollowing", following);
        resp.put("isPrivateAndNotFollowing", privateBlocked);
        if (!privateBlocked) {
            List<Post> posts = postRepository.findByUserAndPostTypeNotOrderByCreatedAtDesc(target, "STORY");
            resp.put("posts", posts.stream().map(this::postDto).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (body.containsKey("bio")) user.setBio((String) body.get("bio"));
        if (body.containsKey("aboutMe")) user.setAboutMe((String) body.get("aboutMe"));
        if (body.containsKey("skills")) user.setSkills((String) body.get("skills"));
        if (body.containsKey("collegeName")) user.setCollegeName((String) body.get("collegeName"));
        if (body.containsKey("gender")) user.setGender((String) body.get("gender"));
        if (body.containsKey("privateAccount")) user.setPrivateAccount(Boolean.TRUE.equals(body.get("privateAccount")));
        if (body.containsKey("profilePhotoUrl")) user.setProfilePhotoUrl((String) body.get("profilePhotoUrl"));
        userRepository.save(user);
        return ResponseEntity.ok(userDto(user));
    }

    @PostMapping("/profile/{id}/follow")
    @Transactional
    public ResponseEntity<?> follow(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User target = userRepository.findById(id).orElse(null);
        if (target == null) return ResponseEntity.notFound().build();
        if (user.getId().equals(target.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot follow yourself"));
        }
        if (target.isPrivateAccount()) {
            Optional<FollowRequest> existing = followRequestRepository.findBySenderAndReceiver(user, target);
            if (existing.isPresent()) {
                return ResponseEntity.ok(Map.of("following", false, "requested", true));
            }
            followRequestRepository.save(new FollowRequest(user, target));
            notificationRepository.save(new Notification(
                    target,
                    user,
                    user.getUsername() + " sent you a follow request",
                    "FOLLOW_REQUEST"
            ));
            return ResponseEntity.ok(Map.of("following", false, "requested", true));
        }
        user.getFollowing().add(target);
        target.getFollowers().add(user);
        userRepository.save(user);
        userRepository.save(target);
        Notification n = new Notification(target, user, user.getUsername() + " started following you", "FOLLOW");
        notificationRepository.save(n);
        return ResponseEntity.ok(Map.of("following", true, "requested", false));
    }

    @PostMapping("/profile/{id}/unfollow")
    @Transactional
    public ResponseEntity<?> unfollow(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User target = userRepository.findById(id).orElse(null);
        if (target == null) return ResponseEntity.notFound().build();
        user.getFollowing().remove(target);
        target.getFollowers().remove(user);
        userRepository.save(user);
        userRepository.save(target);
        followRequestRepository.deleteBySenderAndReceiver(user, target);
        return ResponseEntity.ok(Map.of("following", false));
    }

    @GetMapping("/profile/follow-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> followRequests(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> list = followRequestRepository.findByReceiverOrderByCreatedAtDesc(user)
                .stream().map(this::followRequestDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/profile/follow-requests/{id}/accept")
    @Transactional
    public ResponseEntity<?> acceptFollowRequest(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        FollowRequest fr = followRequestRepository.findById(id).orElse(null);
        if (fr == null || fr.getReceiver() == null || !fr.getReceiver().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        User sender = fr.getSender();
        if (sender != null) {
            user.getFollowers().add(sender);
            sender.getFollowing().add(user);
            userRepository.save(user);
            userRepository.save(sender);
            notificationRepository.save(new Notification(
                    sender,
                    user,
                    user.getUsername() + " accepted your follow request",
                    "FOLLOW_ACCEPTED"
            ));
        }
        followRequestRepository.delete(fr);
        return ResponseEntity.ok(Map.of("message", "Follow request accepted"));
    }

    @PostMapping("/profile/follow-requests/{id}/reject")
    @Transactional
    public ResponseEntity<?> rejectFollowRequest(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        FollowRequest fr = followRequestRepository.findById(id).orElse(null);
        if (fr == null || fr.getReceiver() == null || !fr.getReceiver().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        followRequestRepository.delete(fr);
        return ResponseEntity.ok(Map.of("message", "Follow request rejected"));
    }

    @GetMapping("/profile/collaboration-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> collaborationRequests(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> pending = postCollaborationRepository
                .findByUserAndStatusOrderByPostCreatedAtDesc(user, CollaborationStatus.PENDING)
                .stream().map(this::collaborationDto).collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/profile/collaboration/{id}/accept")
    @Transactional
    public ResponseEntity<?> acceptCollaboration(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        PostCollaboration collab = postCollaborationRepository.findById(id).orElse(null);
        if (collab == null || collab.getUser() == null || !collab.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        collab.setStatus(CollaborationStatus.ACCEPTED);
        postCollaborationRepository.save(collab);
        return ResponseEntity.ok(Map.of("message", "Collaboration accepted"));
    }

    @PostMapping("/profile/collaboration/{id}/reject")
    @Transactional
    public ResponseEntity<?> rejectCollaboration(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        PostCollaboration collab = postCollaborationRepository.findById(id).orElse(null);
        if (collab == null || collab.getUser() == null || !collab.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        collab.setStatus(CollaborationStatus.REJECTED);
        postCollaborationRepository.save(collab);
        return ResponseEntity.ok(Map.of("message", "Collaboration rejected"));
    }

    // ── Events ────────────────────────────────────────────────────────────

    @GetMapping("/events")
    public ResponseEntity<?> events(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        List<Event> all = (search != null && !search.isBlank())
                ? eventRepository.searchByVenueOrTitle(search.trim())
                : eventRepository.findAll();
        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) {
            all = all.stream().filter(e -> category.equalsIgnoreCase(e.getCategory())).collect(Collectors.toList());
        }

        List<Map<String, Object>> voting = all.stream()
                .filter(e -> "VOTING".equals(e.getStatus()))
                .filter(e -> e.getVotingEndDate() == null || e.getVotingEndDate().isAfter(LocalDateTime.now()))
                .map(this::eventDto).collect(Collectors.toList());

        LocalDateTime eightDaysAgo = LocalDateTime.now().minusDays(8);
        List<Map<String, Object>> regular = all.stream()
                .filter(e -> {
                    String s = e.getStatus();
                    if ("VOTING".equals(s) || "REJECTED".equals(s) || e.isDeleted()) return false;
                    return e.getDateTime() == null || !e.getDateTime().isBefore(eightDaysAgo);
                })
                .map(this::eventDto).collect(Collectors.toList());

        List<Map<String, Object>> trending = eventRepository.findAll().stream()
                .filter(e -> "UPCOMING".equals(e.getStatus()) || e.getStatus() == null)
                .limit(3).map(this::eventDto).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("events", regular, "votingPolls", voting, "trending", trending));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<?> eventDetail(@PathVariable Long id, HttpSession session) {
        if (currentUser(session) == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Event e = eventRepository.findById(id).orElse(null);
        if (e == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(eventDto(e));
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<?> ticket(@PathVariable String ticketId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        EventRegistration reg = eventRegistrationRepository.findByTicketId(ticketId).orElse(null);
        if (reg == null || reg.getUser() == null || !reg.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", reg.getId());
        m.put("ticketId", reg.getTicketId());
        m.put("status", reg.getRegistrationStatus());
        m.put("registrationDate", reg.getRegistrationDate() != null ? reg.getRegistrationDate().toString() : null);
        if (reg.getEvent() != null) m.put("event", eventDto(reg.getEvent()));
        return ResponseEntity.ok(m);
    }

    @PostMapping("/events/{id}/register")
    @Transactional
    public ResponseEntity<?> registerEvent(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();

        boolean already = eventRegistrationRepository.findByUser(user).stream()
                .anyMatch(r -> r.getEvent() != null && r.getEvent().getId().equals(id));
        if (already) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already registered"));
        }

        EventRegistration reg = new EventRegistration();
        reg.setUser(user);
        reg.setEvent(event);
        reg.setRegistrationStatus("REGISTERED");
        reg.setRegistrationDate(LocalDateTime.now());
        reg.setTicketId("TIX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        reg.setFullName(user.getUsername());
        reg.setEmail(user.getEmail());
        eventRegistrationRepository.save(reg);
        return ResponseEntity.ok(Map.of(
                "message", "Registered successfully",
                "registrationId", reg.getId(),
                "ticketId", reg.getTicketId()
        ));
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> bookings(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> list = eventRegistrationRepository.findByUserOrderByRegistrationDateDesc(user)
                .stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("status", r.getRegistrationStatus());
                    m.put("ticketId", r.getTicketId());
                    m.put("registrationDate", r.getRegistrationDate() != null ? r.getRegistrationDate().toString() : null);
                    if (r.getEvent() != null) m.put("event", eventDto(r.getEvent()));
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/bookings/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelBooking(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        EventRegistration reg = eventRegistrationRepository.findById(id).orElse(null);
        if (reg == null || reg.getUser() == null || !reg.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        reg.setRegistrationStatus("CANCELLED");
        eventRegistrationRepository.save(reg);
        return ResponseEntity.ok(Map.of("message", "Booking cancelled"));
    }

    // ── Battles ───────────────────────────────────────────────────────────

    @GetMapping("/battles")
    @Transactional(readOnly = true)
    public ResponseEntity<?> battles(HttpSession session) {
        if (currentUser(session) == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Battle> all = battleRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> active = all.stream()
                .filter(b -> "WAITING".equals(b.getStatus()) || "ACTIVE".equals(b.getStatus()) || "VOTING".equals(b.getStatus()))
                .map(this::battleDto).collect(Collectors.toList());
        List<Map<String, Object>> completed = all.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .limit(10).map(this::battleDto).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("active", active, "completed", completed));
    }

    @GetMapping("/battles/{id}")
    public ResponseEntity<?> battleDetail(@PathVariable Long id, HttpSession session) {
        if (currentUser(session) == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Battle b = battleRepository.findById(id).orElse(null);
        if (b == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(battleDto(b));
    }

    // ── Wallet / Shop ─────────────────────────────────────────────────────

    @GetMapping("/wallet")
    public ResponseEntity<?> wallet(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> txs = walletTransactionRepository.findByUserOrderByTimestampDesc(user)
                .stream().map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("amount", t.getAmount());
                    m.put("type", t.getType());
                    m.put("description", t.getDetails());
                    m.put("timestamp", t.getTimestamp() != null ? t.getTimestamp().toString() : null);
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "balance", user.getWalletBalance() != null ? user.getWalletBalance() : 0.0,
                "coins", user.getCoins() != null ? user.getCoins() : 0,
                "transactions", txs
        ));
    }

    @PostMapping("/wallet/add")
    @Transactional
    public ResponseEntity<?> walletAdd(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        double amount = Double.parseDouble(body.get("amount").toString());
        if (amount <= 0) return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount"));
        user.addWalletBalance(amount);
        userRepository.save(user);
        walletTransactionRepository.save(new WalletTransaction(user, amount, "DEPOSIT", "Added funds via mobile"));
        return ResponseEntity.ok(Map.of("balance", user.getWalletBalance()));
    }

    @PostMapping("/wallet/withdraw")
    @Transactional
    public ResponseEntity<?> walletWithdraw(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        double amount = Double.parseDouble(body.get("amount").toString());
        double bal = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        if (amount <= 0 || amount > bal) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient balance"));
        }
        user.addWalletBalance(-amount);
        userRepository.save(user);
        walletTransactionRepository.save(new WalletTransaction(user, amount, "WITHDRAW", "Withdrawn via mobile"));
        return ResponseEntity.ok(Map.of("balance", user.getWalletBalance()));
    }

    @GetMapping("/shop")
    public ResponseEntity<?> shop(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> items = List.of(
                Map.of("id", "discount", "name", "Event Discount", "cost", 100, "owned", user.isHasDiscount()),
                Map.of("id", "boost", "name", "Profile Boost (3 days)", "cost", 200,
                        "owned", user.getProfileBoostUntil() != null && user.getProfileBoostUntil().isAfter(LocalDateTime.now())),
                Map.of("id", "badge", "name", "Premium Badge", "cost", 500, "owned", user.isPremium()),
                Map.of("id", "free_entry", "name", "Free Event Entry", "cost", 300, "owned", user.isHasFreeEntry())
        );
        return ResponseEntity.ok(Map.of("coins", user.getCoins() != null ? user.getCoins() : 0, "items", items));
    }

    @PostMapping("/shop/buy/{itemId}")
    @Transactional
    public ResponseEntity<?> shopBuy(@PathVariable String itemId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        int cost;
        switch (itemId) {
            case "discount" -> {
                cost = 100;
                if (user.getCoins() < cost) return ResponseEntity.badRequest().body(Map.of("error", "Insufficient coins"));
                user.setCoins(user.getCoins() - cost);
                user.setHasDiscount(true);
            }
            case "boost" -> {
                cost = 200;
                if (user.getCoins() < cost) return ResponseEntity.badRequest().body(Map.of("error", "Insufficient coins"));
                user.setCoins(user.getCoins() - cost);
                user.setProfileBoostUntil(LocalDateTime.now().plusDays(3));
            }
            case "badge" -> {
                cost = 500;
                if (user.getCoins() < cost) return ResponseEntity.badRequest().body(Map.of("error", "Insufficient coins"));
                user.setCoins(user.getCoins() - cost);
                user.setPremium(true);
            }
            case "free_entry" -> {
                cost = 300;
                if (user.getCoins() < cost) return ResponseEntity.badRequest().body(Map.of("error", "Insufficient coins"));
                user.setCoins(user.getCoins() - cost);
                user.setHasFreeEntry(true);
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown item"));
            }
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("coins", user.getCoins(), "message", "Purchase successful"));
    }

    // ── Notifications ─────────────────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<?> notifications(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> list = notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(n -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", n.getId());
                    m.put("message", n.getMessage());
                    m.put("type", n.getType());
                    m.put("isRead", n.isRead());
                    m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
                    m.put("postId", n.getPostId());
                    if (n.getActor() != null) {
                        m.put("actorUsername", n.getActor().getUsername());
                        m.put("actorPhoto", n.getActor().getProfilePhotoUrl());
                    }
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/notifications/{id}/delete")
    @Transactional
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n == null || n.getUser() == null || !n.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        notificationRepository.delete(n);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    @PostMapping("/notifications/clear-all")
    @Transactional
    public ResponseEntity<?> clearNotifications(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        notificationRepository.deleteAll(list);
        return ResponseEntity.ok(Map.of("message", "All notifications cleared"));
    }

    @PostMapping("/notifications/mark-all-read")
    @Transactional
    public ResponseEntity<?> markAllRead(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Notification> list = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        for (Notification n : list) n.setRead(true);
        notificationRepository.saveAll(list);
        return ResponseEntity.ok(Map.of("message", "All marked read"));
    }

    // ── Music ─────────────────────────────────────────────────────────────

    @GetMapping("/music/tracks")
    public ResponseEntity<?> tracks(HttpSession session) {
        if (currentUser(session) == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> list = trackRepository.findByStatusOrderByCreatedAtDesc(TrackStatus.APPROVED)
                .stream().map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("title", t.getTitle());
                    m.put("artist", t.getArtistName());
                    m.put("durationSeconds", t.getDurationSeconds());
                    m.put("streamUrl", "/music/stream/" + t.getId());
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/music/rooms")
    @Transactional(readOnly = true)
    public ResponseEntity<?> musicRooms(HttpSession session) {
        if (currentUser(session) == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> list = musicRoomRepository.findAll().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("code", r.getCode());
            m.put("name", r.getName());
            m.put("status", r.getPhase());
            m.put("active", r.isActive());
            m.put("hostUsername", r.getHost() != null ? r.getHost().getUsername() : null);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ── Stories ────────────────────────────────────────────────────────────

    @GetMapping("/stories")
    @Transactional(readOnly = true)
    public ResponseEntity<?> stories(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Set<Long> included = new HashSet<>();
        List<Map<String, Object>> out = new ArrayList<>();
        List<User> candidates = new ArrayList<>();
        candidates.add(user);
        candidates.addAll(user.getFollowing());
        for (User u : candidates) {
            if (u == null || u.getId() == null || included.contains(u.getId())) continue;
            included.add(u.getId());
            List<Post> stories = postRepository.findByUserAndPostTypeAndCreatedAtAfterOrderByCreatedAtAsc(u, "STORY", since);
            if (stories.isEmpty()) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", u.getId());
            item.put("username", u.getUsername());
            item.put("profilePhotoUrl", u.getProfilePhotoUrl());
            item.put("stories", stories.stream().map(this::postDto).collect(Collectors.toList()));
            out.add(item);
        }
        return ResponseEntity.ok(out);
    }

    // ── Rewards / Coupons ─────────────────────────────────────────────────

    @GetMapping("/rewards")
    @Transactional(readOnly = true)
    public ResponseEntity<?> rewards(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> list = userRewardRepository.findByUserOrderByIssueDateDesc(user)
                .stream().map(this::rewardDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/rewards/reveal/{registrationId}")
    @Transactional
    public ResponseEntity<?> revealReward(@PathVariable Long registrationId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        EventRegistration reg = eventRegistrationRepository.findById(registrationId).orElse(null);
        if (reg == null || reg.getUser() == null || !reg.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        if (!reg.isAttendanceMarked() && (reg.getEvent() == null || !"COMPLETED".equals(reg.getEvent().getStatus()))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Attendance not marked yet"));
        }
        if (!reg.isRewardRevealed()) {
            secretRewardService.assignReward(reg);
            reg.setRewardRevealed(true);
            eventRegistrationRepository.save(reg);
        }
        return ResponseEntity.ok(Map.of("message", "Reward revealed"));
    }

    @PostMapping("/rewards/redeem/{rewardCode}")
    @Transactional
    public ResponseEntity<?> redeemReward(@PathVariable String rewardCode, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        UserReward reward = userRewardRepository.findByRewardCode(rewardCode).orElse(null);
        if (reward == null || reward.getUser() == null || !reward.getUser().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid reward code"));
        }
        if (!"AVAILABLE".equals(reward.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reward already redeemed or expired"));
        }
        reward.setStatus("REDEEMED");
        userRewardRepository.save(reward);
        return ResponseEntity.ok(Map.of("message", "Reward redeemed"));
    }

    @PostMapping("/coupon/redeem")
    public ResponseEntity<?> redeemCoupon(@RequestBody Map<String, String> payload, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        String code = payload.getOrDefault("code", "").trim().toUpperCase();
        if (code.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Coupon code is required"));
        if (!"ZENTRIX-ELITE-2025".equals(code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid coupon code"));
        }
        user.setPremium(true);
        user.setCoins((user.getCoins() != null ? user.getCoins() : 0) + 250);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Coupon redeemed", "coins", user.getCoins(), "isPremium", user.isPremium()));
    }

    // ── Achievements (derived from user stats) ────────────────────────────

    @GetMapping("/achievements")
    public ResponseEntity<?> achievements(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        int coins = user.getCoins() != null ? user.getCoins() : 0;
        int xp = user.getXp() != null ? user.getXp() : 0;
        int followers = user.getFollowers() != null ? user.getFollowers().size() : 0;
        List<Map<String, Object>> badges = new ArrayList<>();
        badges.add(Map.of("id", "novice", "title", "Novice", "unlocked", true, "description", "Joined Zentrix"));
        badges.add(Map.of("id", "social", "title", "Social Butterfly", "unlocked", followers >= 10,
                "description", "Reach 10 followers"));
        badges.add(Map.of("id", "coins", "title", "Coin Collector", "unlocked", coins >= 100,
                "description", "Earn 100 Zen coins"));
        badges.add(Map.of("id", "xp", "title", "XP Hunter", "unlocked", xp >= 500,
                "description", "Reach 500 XP"));
        badges.add(Map.of("id", "premium", "title", "Elite", "unlocked", user.isPremium(),
                "description", "Unlock premium badge"));
        return ResponseEntity.ok(Map.of(
                "xp", xp,
                "level", user.getLevel() != null ? user.getLevel() : "Novice",
                "coins", coins,
                "badges", badges
        ));
    }
}
