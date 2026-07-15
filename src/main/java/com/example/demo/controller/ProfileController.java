package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.FollowRequestRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RewardService;
import jakarta.servlet.http.HttpSession;
import com.example.demo.model.UserReward;
import com.example.demo.repository.UserRewardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Controller
@RequestMapping(value = "/profile")
public class ProfileController {

    @Autowired
    private com.example.demo.service.FeedAlgorithmService feedAlgorithmService;

    @Autowired
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    private User getUserFromSession(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return (User) authUser;
        }
        
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private com.example.demo.repository.PostCollaborationRepository postCollaborationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRewardRepository userRewardRepository;

    @Autowired
    private FollowRequestRepository followRequestRepository;

    @Autowired
    private com.example.demo.repository.EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private com.example.demo.repository.UserActivityRepository userActivityRepository;

    @Autowired
    private com.example.demo.config.JwtUtil jwtUtil;


    @Transactional(readOnly = true)
    @RequestMapping(value = "/{username}", method = RequestMethod.GET)
    public String showPublicProfile(@PathVariable String username, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        User targetUser = userRepository.findByUsername(username);
        if (targetUser == null) {
            return "redirect:/dashboard";
        }

        // Refresh current user to get latest following list
        currentUser = userRepository.findById(currentUser.getId()).orElse(currentUser);
        session.setAttribute("user", currentUser);

        // Calculate Talent Score Stats
        long eventsJoined = eventRegistrationRepository.countByUser(targetUser);
        long eventsWon = eventRegistrationRepository.countByUserAndPosition(targetUser, "Winner");

        // Calculate Rank with XP descending and ID ascending as a tie-breaker
        long rank = 1;
        List<User> allUsers = userRepository.findAll();
        allUsers.sort((u1, u2) -> {
            int xp1 = u1.getXp() != null ? u1.getXp() : 0;
            int xp2 = u2.getXp() != null ? u2.getXp() : 0;
            if (xp1 != xp2) {
                return Integer.compare(xp2, xp1);
            }
            return Long.compare(u1.getId(), u2.getId());
        });
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getId().equals(targetUser.getId())) {
                rank = i + 1;
                break;
            }
        }

        String badge = targetUser.getLevel() != null ? targetUser.getLevel() : "Novice";

        boolean isOwnProfile = currentUser.getId().equals(targetUser.getId());
        model.addAttribute("user", targetUser);
        model.addAttribute("isOwnProfile", isOwnProfile);

        // Add Talent Score to model
        long talentScore = (eventsJoined * 10) + (eventsWon * 50) + (targetUser.getXp() != null ? targetUser.getXp() : 0);
        model.addAttribute("talentScore", talentScore);
        model.addAttribute("eventsJoined", eventsJoined);
        model.addAttribute("eventsWon", eventsWon);
        model.addAttribute("userRank", rank);
        model.addAttribute("userBadge", badge);
        boolean isFollowing = currentUser.getFollowing().contains(targetUser);
        model.addAttribute("isFollowing", isFollowing);

        boolean hasSentFollowRequest = followRequestRepository.findBySenderAndReceiver(currentUser, targetUser).isPresent();
        model.addAttribute("hasSentFollowRequest", hasSentFollowRequest);

        boolean isPrivateAndNotFollowing = targetUser.isPrivateAccount() && !isOwnProfile && !isFollowing;
        model.addAttribute("isPrivateAndNotFollowing", isPrivateAndNotFollowing);

        model.addAttribute("followersCount", targetUser.getFollowers().size());
        model.addAttribute("followingCount", targetUser.getFollowing().size());

        // Fetch posts to get the count
        List<Post> posts = postRepository.findByUserAndPostTypeNotOrderByCreatedAtDesc(targetUser, "STORY");
        List<com.example.demo.model.PostCollaboration> collaborations = postCollaborationRepository
                .findByUserAndStatus(targetUser, com.example.demo.model.CollaborationStatus.ACCEPTED);
        for (com.example.demo.model.PostCollaboration col : collaborations) {
            posts.add(col.getPost());
        }
        posts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

        model.addAttribute("postsCount", posts.size());

        if (isPrivateAndNotFollowing) {
            model.addAttribute("followers", new java.util.HashSet<>());
            model.addAttribute("following", new java.util.HashSet<>());
            model.addAttribute("posts", new java.util.ArrayList<>());
        } else {
            model.addAttribute("followers", targetUser.getFollowers());
            model.addAttribute("following", targetUser.getFollowing());

            // Initialize lazy collaborations for posts
            for (Post post : posts) {
                if (post.getCollaborations() != null) {
                    post.getCollaborations().size();
                }
            }
            model.addAttribute("posts", posts);
        }

        // Check for active stories
        boolean hasStory = !postRepository.findByUserAndPostTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                targetUser, "STORY", java.time.LocalDateTime.now().minusHours(24)).isEmpty();
        model.addAttribute("hasStory", hasStory);

        if (isOwnProfile) {
            List<com.example.demo.model.PostCollaboration> pendingRequests = postCollaborationRepository
                    .findByUserAndStatus(currentUser, com.example.demo.model.CollaborationStatus.PENDING);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("notifications", notificationRepository.findByUserOrderByCreatedAtDesc(currentUser));
            model.addAttribute("unreadNotifCount", notificationRepository.countByUserAndIsRead(currentUser, false));
            final Long currentUserId = currentUser.getId();
            model.addAttribute("followRequests", followRequestRepository.findAll().stream()
                    .filter(r -> r.getReceiver().getId().equals(currentUserId))
                    .collect(java.util.stream.Collectors.toList()));

            Set<Long> followingUserIds = currentUser.getFollowing().stream()
                    .map(User::getId)
                    .collect(java.util.stream.Collectors.toSet());
            model.addAttribute("followingUserIds", followingUserIds);

            // Fetch saved posts
            List<UserActivity> activities = userActivityRepository.findByUserId(currentUser.getId());
            List<Post> savedPosts = activities.stream()
                    .filter(a -> a.getActivityType() == ActivityType.SAVE)
                    .map(UserActivity::getPost)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            // Initialize lazy collaborations for saved posts
            for (Post post : savedPosts) {
                if (post.getCollaborations() != null) {
                    post.getCollaborations().size();
                }
            }
            model.addAttribute("savedPosts", savedPosts);

            // Fetch user rewards
            List<UserReward> userRewards = userRewardRepository.findByUserOrderByIssueDateDesc(currentUser);
            // Initialize lazy secretReward for rewards
            for (UserReward reward : userRewards) {
                if (reward.getSecretReward() != null) {
                    reward.getSecretReward().getId(); // Triggers loading the lazy relationship
                }
            }
            model.addAttribute("userRewards", userRewards);
        } else {
            model.addAttribute("pendingRequests", new java.util.ArrayList<>());
            model.addAttribute("notifications", new java.util.ArrayList<>());
            model.addAttribute("unreadNotifCount", 0);
            model.addAttribute("followRequests", new java.util.ArrayList<>());
            model.addAttribute("followingUserIds", new java.util.HashSet<>());
            model.addAttribute("savedPosts", new java.util.ArrayList<>());
            model.addAttribute("userRewards", new java.util.ArrayList<>());
        }

        return "profile";
    }

    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        return "redirect:/profile/" + user.getUsername();
    }

    @Transactional
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public String updateProfile(@RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String dob,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String profilePhotoUrl,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile profilePhotoFile,
            @RequestParam(required = false) String aboutMe,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String collegeName,
            @RequestParam(required = false, defaultValue = "false") boolean privateAccount,
            HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        Object sessionUser = session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            return "redirect:/login";
        }
        User user = (User) sessionUser;

        try {
            User dbUser = userRepository.findById(user.getId()).orElse(null);
            if (dbUser != null) {
                String oldUsername = dbUser.getUsername();
                dbUser.setUsername(username);
                dbUser.setEmail(email);
                if (dob != null && !dob.trim().isEmpty()) {
                    java.time.LocalDate birthDate = java.time.LocalDate.parse(dob);
                    if (birthDate.isAfter(java.time.LocalDate.now())) {
                        System.err.println("Attempted to set future date of birth: " + dob);
                    } else {
                        dbUser.setDob(birthDate);
                    }
                }
                if (gender != null)
                    dbUser.setGender(gender);

                if (profilePhotoFile != null && !profilePhotoFile.isEmpty()) {
                    String contentType = profilePhotoFile.getContentType();
                    if (contentType != null && contentType.startsWith("image")) {
                        try {
                            String fileName = java.util.UUID.randomUUID().toString() + "_" + profilePhotoFile.getOriginalFilename();
                            String uploadDir = "src/main/resources/static/uploads/";
                            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                            if (!java.nio.file.Files.exists(uploadPath)) {
                                java.nio.file.Files.createDirectories(uploadPath);
                            }
                            String targetUploadDir = "target/classes/static/uploads/";
                            java.nio.file.Path targetUploadPath = java.nio.file.Paths.get(targetUploadDir);
                            if (!java.nio.file.Files.exists(targetUploadPath)) {
                                java.nio.file.Files.createDirectories(targetUploadPath);
                            }
                            java.nio.file.Files.copy(profilePhotoFile.getInputStream(), uploadPath.resolve(fileName),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            java.nio.file.Files.copy(profilePhotoFile.getInputStream(), targetUploadPath.resolve(fileName),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            dbUser.setProfilePhotoUrl("/uploads/" + fileName);
                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (profilePhotoUrl != null) {
                    dbUser.setProfilePhotoUrl(profilePhotoUrl.length() > 255 ? profilePhotoUrl.substring(0, 255) : profilePhotoUrl);
                }
                if (aboutMe != null)
                    dbUser.setAboutMe(aboutMe.length() > 1000 ? aboutMe.substring(0, 1000) : aboutMe);
                if (skills != null)
                    dbUser.setSkills(skills.length() > 255 ? skills.substring(0, 255) : skills);
                if (collegeName != null)
                    dbUser.setCollegeName(collegeName.length() > 255 ? collegeName.substring(0, 255) : collegeName);
                dbUser.setPrivateAccount(privateAccount);
                userRepository.save(dbUser);
                
                // If username is changed, generate a new token and update the client-side cookie
                if (!oldUsername.equals(username)) {
                    String newToken = jwtUtil.generateToken(username);
                    jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", newToken);
                    cookie.setHttpOnly(true);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
                
                // Force reload with collections if needed, or just set the basic user
                session.setAttribute("user", dbUser);
            }
            return "redirect:/profile?success";
        } catch (Exception e) {
            System.err.println("Error updating profile for user: " + username);
            e.printStackTrace();
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            try {
                return "redirect:/profile?error=" + java.net.URLEncoder.encode(errorMessage, "UTF-8");
            } catch (java.io.UnsupportedEncodingException ex) {
                return "redirect:/profile?error=encoding_error";
            }
        }
    }

    @RequestMapping(value = "/reset-password", method = RequestMethod.POST)
    public String resetPassword(@RequestParam String newPassword, HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            return "redirect:/login";
        }
        User user = (User) sessionUser;

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser != null) {
            dbUser.setPassword(newPassword);
            userRepository.save(dbUser);
        }
        return "redirect:/profile?passwordReset";
    }

    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public String createPost(@RequestParam String content,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(required = false) String hashtags,
            @RequestParam(required = false) String collaborators,
            @RequestParam(required = false, defaultValue = "POST") String postType,
            @RequestParam(required = false) String category,
            HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            return "redirect:/login";
        }
        User user = (User) sessionUser;

        String mediaUrl = null;
        String mediaType = null;

        if (file != null && !file.isEmpty()) {
            try {
                String fileName = java.util.UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                String uploadDir = "src/main/resources/static/uploads/";
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }

                // Also save to target to be immediately available without restart in some
                // setups
                String targetUploadDir = "target/classes/static/uploads/";
                java.nio.file.Path targetUploadPath = java.nio.file.Paths.get(targetUploadDir);
                if (!java.nio.file.Files.exists(targetUploadPath)) {
                    java.nio.file.Files.createDirectories(targetUploadPath);
                }

                java.nio.file.Files.copy(file.getInputStream(), uploadPath.resolve(fileName),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.copy(file.getInputStream(), targetUploadPath.resolve(fileName),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                mediaUrl = "/uploads/" + fileName;
                String contentType = file.getContentType();
                if (contentType != null && contentType.startsWith("video")) {
                    mediaType = "VIDEO";
                } else {
                    mediaType = "IMAGE";
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }

        if (user != null) {
            user = userRepository.findById(user.getId()).orElse(null);
        }

        if (user == null) {
            return "redirect:/login";
        }

        Post post = new Post(content, user, mediaUrl, mediaType, hashtags, postType, category);
        feedAlgorithmService.savePost(post);

        // Handle collaborators (both mentions and explicit tags)
        Set<User> collaboratorSet = new HashSet<>();

        // 1. Mentions (@username)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String uname = matcher.group(1);
            User u = userRepository.findByUsername(uname);
            if (u != null && !u.getId().equals(user.getId())) {
                collaboratorSet.add(u);
            }
        }

        // 2. Explicit collaborators from the UI tagger
        if (collaborators != null && !collaborators.isEmpty()) {
            for (String uname : collaborators.split(",")) {
                User u = userRepository.findByUsername(uname.trim());
                if (u != null && !u.getId().equals(user.getId())) {
                    collaboratorSet.add(u);
                }
            }
        }

        // Save all collaborations
        for (User collabUser : collaboratorSet) {
            com.example.demo.model.PostCollaboration collaboration = new com.example.demo.model.PostCollaboration(
                    post, collabUser, com.example.demo.model.CollaborationStatus.PENDING);
            postCollaborationRepository.save(collaboration);
        }
        rewardService.awardTalentPost(user); // Coins for Posting 🎨
        return "redirect:/profile";
    }

    @RequestMapping(value = "/post/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deletePost(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        // Security check: Only the owner can delete
        if (!post.getUser().getId().equals(user.getId())) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .build();
        }

        feedAlgorithmService.deletePost(post);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Transactional
    @RequestMapping(value = "/{id}/follow", method = RequestMethod.POST)
    public String followUser(@PathVariable Long id, HttpSession session,
            @RequestHeader(value = "Referer", required = false) String referer) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return "redirect:/login";

        User dbTargetUser = userRepository.findById(id).orElse(null);
        User dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(null);

        if (dbCurrentUser != null && dbTargetUser != null
                && !dbTargetUser.getId().equals(dbCurrentUser.getId())) {

            // Check if already following
            if (dbTargetUser.getFollowers().contains(dbCurrentUser)) {
                return (referer != null) ? "redirect:" + referer : "redirect:/profile";
            }

            // Check if request already exists
            if (followRequestRepository.findBySenderAndReceiver(dbCurrentUser, dbTargetUser).isPresent()) {
                return (referer != null) ? "redirect:" + referer : "redirect:/profile";
            }

            if (!dbTargetUser.isPrivateAccount()) {
                dbTargetUser.getFollowers().add(dbCurrentUser);
                dbCurrentUser.getFollowing().add(dbTargetUser);
                userRepository.save(dbTargetUser);
                userRepository.save(dbCurrentUser);
                notificationRepository.save(new Notification(dbTargetUser, dbCurrentUser,
                        "@" + dbCurrentUser.getUsername() + " started following you!", "FOLLOW_ACCEPT"));
            } else {
                // Create FollowRequest
                FollowRequest request = new FollowRequest(dbCurrentUser, dbTargetUser);
                followRequestRepository.save(request);

                // Create Notification
                Notification notif = new Notification(dbTargetUser, dbCurrentUser,
                        "@" + dbCurrentUser.getUsername() + " wants to follow you!", "FOLLOW_REQUEST");
                notificationRepository.save(notif);
            }
        }
        return (referer != null) ? "redirect:" + referer : "redirect:/profile";
    }

    @Transactional
    @RequestMapping(value = "/{id}/follow/ajax", method = RequestMethod.POST)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> followUserAjax(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .build();
        }

        User dbTargetUser = userRepository.findById(id).orElse(null);
        User dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(null);

        if (dbCurrentUser != null && dbTargetUser != null) {
            if (dbTargetUser.getId().equals(dbCurrentUser.getId())) {
                return org.springframework.http.ResponseEntity.badRequest().body("You cannot follow yourself.");
            }

            // Check if already following (ID-based check for proxy safety)
            final Long currentUserId = dbCurrentUser.getId();
            boolean alreadyFollowing = dbTargetUser.getFollowers().stream()
                    .anyMatch(f -> f.getId().equals(currentUserId));
            if (alreadyFollowing) {
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "FOLLOWING"));
            }

            // Direct follow if target follows current user (Follow Back) AND target is not private
            final Long targetUserId = dbTargetUser.getId();
            boolean targetFollowsMe = dbCurrentUser.getFollowers().stream()
                    .anyMatch(f -> f.getId().equals(targetUserId));
            if (targetFollowsMe && !dbTargetUser.isPrivateAccount()) {
                dbTargetUser.getFollowers().add(dbCurrentUser);
                dbCurrentUser.getFollowing().add(dbTargetUser); // Bidirectional update
                userRepository.save(dbTargetUser);
                userRepository.save(dbCurrentUser);

                // Also create a follow-back notification
                notificationRepository.save(new Notification(dbTargetUser, dbCurrentUser,
                        "@" + dbCurrentUser.getUsername() + " followed you back!", "FOLLOW_ACCEPT"));

                return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "FOLLOWING"));
            }

            // Otherwise, process based on privacy
            java.util.Optional<FollowRequest> existingReq = followRequestRepository
                    .findBySenderAndReceiver(dbCurrentUser, dbTargetUser);
            if (existingReq.isEmpty()) {
                if (!dbTargetUser.isPrivateAccount()) {
                    // Direct follow for public accounts
                    dbTargetUser.getFollowers().add(dbCurrentUser);
                    dbCurrentUser.getFollowing().add(dbTargetUser);
                    userRepository.save(dbTargetUser);
                    userRepository.save(dbCurrentUser);
                    notificationRepository.save(new Notification(dbTargetUser, dbCurrentUser,
                            "@" + dbCurrentUser.getUsername() + " started following you!", "FOLLOW_ACCEPT"));
                    return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "FOLLOWING"));
                } else {
                    // Follow request for private accounts
                    FollowRequest request = new FollowRequest(dbCurrentUser, dbTargetUser);
                    followRequestRepository.save(request);

                    Notification notif = new Notification(dbTargetUser, dbCurrentUser,
                            "@" + dbCurrentUser.getUsername() + " wants to follow you!", "FOLLOW_REQUEST");
                    notificationRepository.save(notif);
                    return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "REQUESTED"));
                }
            } else {
                // If it already exists, we return REQUESTED so the frontend knows it's there.
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "REQUESTED"));
            }
        }
        return org.springframework.http.ResponseEntity.badRequest()
                .body("Failed to process request. Current User: " + (dbCurrentUser != null ? "found" : "null")
                        + ", Target User: " + (dbTargetUser != null ? "found" : "null"));
    }

    @Transactional
    @RequestMapping(value = "/{id}/cancel-follow/ajax", method = RequestMethod.POST)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> cancelFollowAjax(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return org.springframework.http.ResponseEntity.status(401).build();

        User dbTargetUser = userRepository.findById(id).orElse(null);
        User dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(null);

        if (dbCurrentUser != null && dbTargetUser != null) {
            followRequestRepository.findBySenderAndReceiver(dbCurrentUser, dbTargetUser).ifPresent(fr -> {
                followRequestRepository.delete(fr);
                // Also remove the notification if possible (optional but cleaner)
                notificationRepository.deleteByActorAndUserAndType(dbCurrentUser, dbTargetUser, "FOLLOW_REQUEST");
            });
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "FOLLOW"));
        }
        return org.springframework.http.ResponseEntity.badRequest().build();
    }

    @Transactional
    @RequestMapping(value = "/{id}/unfollow", method = RequestMethod.POST)
    public String unfollowUser(@PathVariable Long id, HttpSession session,
            @RequestHeader(value = "Referer", required = false) String referer) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return "redirect:/login";

        User dbTargetUser = userRepository.findById(id).orElse(null);
        User dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(null);

        if (dbCurrentUser != null && dbTargetUser != null) {
            dbTargetUser.getFollowers().remove(dbCurrentUser);
            userRepository.save(dbTargetUser);
            // Refresh session
            dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(dbCurrentUser);
            session.setAttribute("user", dbCurrentUser);
        }
        return (referer != null) ? "redirect:" + referer : "redirect:/profile";
    }

    @Transactional
    @RequestMapping(value = "/{id}/unfollow/ajax", method = RequestMethod.POST)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> unfollowUserAjax(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        User dbTargetUser = userRepository.findById(id).orElse(null);
        User dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(null);

        if (dbCurrentUser != null && dbTargetUser != null) {
            // Remove from both sides to ensure consistency
            dbTargetUser.getFollowers().remove(dbCurrentUser);
            dbCurrentUser.getFollowing().remove(dbTargetUser);
            userRepository.save(dbTargetUser);
            userRepository.save(dbCurrentUser);

            // Clean up follow notification
            notificationRepository.deleteByActorAndUserAndType(dbCurrentUser, dbTargetUser, "FOLLOW_ACCEPT");

            // Refresh session user
            session.setAttribute("user", dbCurrentUser);

            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "FOLLOW"));
        }
        return org.springframework.http.ResponseEntity.badRequest().build();
    }

    @RequestMapping(value = "/api/users/search", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> searchUsers(@RequestParam String q) {
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(q);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("username", u.getUsername());
            map.put("profilePhotoUrl", u.getProfilePhotoUrl());
            result.add(map);
        }
        return result;
    }

    @RequestMapping(value = "/collaboration/{id}/accept", method = RequestMethod.POST)
    public String acceptCollaboration(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        com.example.demo.model.PostCollaboration collaboration = postCollaborationRepository.findById(id).orElse(null);
        if (collaboration != null && collaboration.getUser().getId().equals(user.getId())) {
            collaboration.setStatus(com.example.demo.model.CollaborationStatus.ACCEPTED);
            postCollaborationRepository.save(collaboration);
        }
        return "redirect:/profile";
    }

    @RequestMapping(value = "/collaboration/{id}/reject", method = RequestMethod.POST)
    public String rejectCollaboration(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        com.example.demo.model.PostCollaboration collaboration = postCollaborationRepository.findById(id).orElse(null);
        if (collaboration != null && collaboration.getUser().getId().equals(user.getId())) {
            collaboration.setStatus(com.example.demo.model.CollaborationStatus.REJECTED);
            postCollaborationRepository.save(collaboration);
        }
        return "redirect:/profile";
    }

    @Transactional
    @RequestMapping(value = "/notifications/mark-all-read", method = RequestMethod.POST)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> markAllNotificationsRead(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }
        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser != null) {
            List<Notification> unread = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(dbUser, false);
            for (Notification n : unread) {
                n.setRead(true);
            }
            notificationRepository.saveAll(unread);
        }
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Transactional
    @RequestMapping(value = "/follow-request/{id}/accept/ajax", method = RequestMethod.POST)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> acceptFollowAjax(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return org.springframework.http.ResponseEntity.status(401).build();

        // Try as Notification ID first (common in notifications list)
        Notification notif = notificationRepository.findById(id).orElse(null);
        if (notif != null && "FOLLOW_REQUEST".equals(notif.getType())) {
            User sender = notif.getActor();
            User receiver = notif.getUser();
            if (receiver.getId().equals(user.getId())) {
                FollowRequest fr = followRequestRepository.findBySenderAndReceiver(sender, receiver).orElse(null);
                if (fr != null) {
                    receiver.getFollowers().add(sender);
                    sender.getFollowing().add(receiver); // Bidirectional update
                    userRepository.save(receiver);
                    userRepository.save(sender);

                    followRequestRepository.delete(fr);
                    notificationRepository.delete(notif);

                    notificationRepository.save(new Notification(sender, receiver,
                            "@" + receiver.getUsername() + " accepted your follow request!", "FOLLOW_ACCEPT"));

                    final Long senderId = sender.getId();
                    boolean followsBack = receiver.getFollowing().stream().anyMatch(u -> u.getId().equals(senderId));
                    return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                            "status", "ACCEPTED", "needsFollowBack", !followsBack,
                            "senderId", sender.getId(), "senderUsername", sender.getUsername()));
                } else {
                    // Orphaned notification handling: gracefully delete and fake success
                    notificationRepository.delete(notif);
                    return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                            "status", "ALREADY_PROCESSED"));
                }
            }
        }

        // Fallback or legacy support: check if it's a direct FollowRequest ID
        FollowRequest fr = followRequestRepository.findById(id).orElse(null);
        if (fr != null && fr.getReceiver().getId().equals(user.getId())) {
            User sender = fr.getSender();
            User receiver = fr.getReceiver();
            receiver.getFollowers().add(sender);
            sender.getFollowing().add(receiver); // Bidirectional update
            userRepository.save(receiver);
            userRepository.save(sender);

            followRequestRepository.delete(fr);
            notificationRepository.deleteByActorAndUserAndType(sender, receiver, "FOLLOW_REQUEST");

            notificationRepository.save(new Notification(sender, receiver,
                    "@" + receiver.getUsername() + " accepted your follow request!", "FOLLOW_ACCEPT"));

            final Long senderId = sender.getId();
            boolean followsBack = receiver.getFollowing().stream().anyMatch(u -> u.getId().equals(senderId));
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "status", "ACCEPTED", "needsFollowBack", !followsBack,
                    "senderId", sender.getId(), "senderUsername", sender.getUsername()));
        }
        return org.springframework.http.ResponseEntity.badRequest().build();
    }

    @Transactional
    @RequestMapping(value = "/follow-request/{id}/reject/ajax", method = RequestMethod.POST)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> rejectFollowAjax(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return org.springframework.http.ResponseEntity.status(401).build();

        Notification notif = notificationRepository.findById(id).orElse(null);
        if (notif != null && "FOLLOW_REQUEST".equals(notif.getType())) {
            followRequestRepository.deleteBySenderAndReceiver(notif.getActor(), notif.getUser());
            notif.setRead(true);
            notificationRepository.save(notif);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "REJECTED"));
        }

        FollowRequest fr = followRequestRepository.findById(id).orElse(null);
        if (fr != null && fr.getReceiver().getId().equals(user.getId())) {
            followRequestRepository.delete(fr);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "REJECTED"));
        }
        return org.springframework.http.ResponseEntity.badRequest().build();
    }

    @Transactional
    @RequestMapping(value = "/follow-request/{id}/accept", method = RequestMethod.POST)
    public String acceptFollow(@PathVariable Long id, HttpSession session) {
        acceptFollowAjax(id, session);
        return "redirect:/profile";
    }

    @Transactional
    @RequestMapping(value = "/follow-request/{id}/reject", method = RequestMethod.POST)
    public String rejectFollow(@PathVariable Long id, HttpSession session) {
        rejectFollowAjax(id, session);
        return "redirect:/profile";
    }

    // Endpoints moved to NotificationController.java
}
