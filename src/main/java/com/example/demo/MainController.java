package com.example.demo;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.config.JwtUtil;
import com.example.demo.config.TokenBlacklist;
import com.example.demo.music.room.MusicRoom;
import com.example.demo.music.room.MusicRoomRepository;
import com.example.demo.service.FeedAlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class MainController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    /**
     * Validates the jwtToken cookie on PUBLIC pages (which bypass the AuthInterceptor).
     * If the token is missing, blacklisted, expired, or invalid — the session user
     * is cleared so the navbar shows Login/Register instead of Dashboard/Logout.
     */
    private void validateSessionOnPublicPage(HttpSession session, HttpServletRequest request) {
        // Only matters if a session user is set
        Object sessionUser = session.getAttribute("user");
        if (sessionUser == null) return;

        // Find the JWT cookie
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("jwtToken".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        // No cookie at all → browser session ended / user never logged in properly
        if (token == null || token.isBlank()) {
            session.removeAttribute("user");
            session.removeAttribute("userId");
            return;
        }

        // Token was explicitly revoked (logged out from another tab/window)
        if (tokenBlacklist.isBlacklisted(token)) {
            session.removeAttribute("user");
            session.removeAttribute("userId");
            return;
        }

        // Token is structurally invalid or expired
        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                session.removeAttribute("user");
                session.removeAttribute("userId");
            }
        } catch (Exception e) {
            // Malformed / expired token
            session.removeAttribute("user");
            session.removeAttribute("userId");
        }
    }

    private User getUserFromSession(HttpSession session) {
        // First check the request attribute (set by Interceptor)
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return (User) authUser;
        }

        // Fallback to session (existing logic)
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

        // Fallback to JWT Cookie (for game pages where AuthInterceptor is bypassed)
        String token = null;
        if (httpServletRequest.getCookies() != null) {
            for (Cookie c : httpServletRequest.getCookies()) {
                if ("jwtToken".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token != null && !token.isBlank() && !tokenBlacklist.isBlacklisted(token)) {
            try {
                String username = jwtUtil.extractUsername(token);
                if (username != null) {
                    User user = userRepository.findByUsername(username);
                    if (user != null && jwtUtil.validateToken(token, username)) {
                        session.setAttribute("user", user);
                        session.setAttribute("userId", user.getId());
                        return user;
                    }
                }
            } catch (Exception e) {}
        }
        return null;
    }

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FeedAlgorithmService feedAlgorithmService;

    @Autowired
    private PostCollaborationRepository postCollaborationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.demo.service.RewardService rewardService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private com.example.demo.repository.EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private FollowRequestRepository followRequestRepository;

    @Autowired
    private MusicRoomRepository musicRoomRepository;

    @Autowired
    private com.example.demo.repository.GameRepository gameRepository;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleParticipantRepository battleParticipantRepository;

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model, HttpSession session, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        validateSessionOnPublicPage(session, request);

        model.addAttribute("user", getUserFromSession(session));
        // Fetch real student thoughts
        model.addAttribute("thoughts", postRepository.findByPostTypeOrderByCreatedAtDesc("THOUGHT"));
        // Fetch all games
        model.addAttribute("games", gameRepository.findAll());
        // Fetch all public events (UPCOMING, ONGOING, VOTING)
        model.addAttribute("events", eventRepository.findByStatusInOrderByCreatedAtDesc(
                java.util.List.of("UPCOMING", "ONGOING", "VOTING")));
        return "home";
    }

    @org.springframework.web.bind.annotation.PostMapping("/home/thought")
    public String shareThought(
            @org.springframework.web.bind.annotation.RequestParam String content,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "Community") String category,
            HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        // Refresh user from DB
        user = userRepository.findById(user.getId()).orElse(user);
        com.example.demo.model.Post post = new com.example.demo.model.Post(
                content, user, null, null, null, "THOUGHT", category);
        feedAlgorithmService.savePost(post);
        return "redirect:/home?thoughtShared=true";
    }

    @GetMapping("/featured-events")
    public String featuredEvents() {
        return "redirect:/home#featured-events";
    }

    @GetMapping("/categories")
    public String categories() {
        return "redirect:/home#categories";
    }

    @GetMapping("/about-us")
    public String aboutUs(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "about";
    }

    @GetMapping("/careers")
    public String careers(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "careers";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "privacy";
    }

    @GetMapping("/terms-of-service")
    public String termsOfService(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "terms";
    }

    @GetMapping("/features")
    public String features() {
        return "features";
    }

    @GetMapping("/support")
    public String support(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "support";
    }

    @GetMapping("/about")
    public String about(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "about";
    }

    @GetMapping("/faq")
    public String faq(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "faq";
    }

    @GetMapping("/terms")
    public String terms(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "privacy";
    }

    @GetMapping("/organizers")
    public String organizers(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "organizers";
    }

    @GetMapping("/games")
    public String games(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "games";
    }

    @GetMapping("/games/rock-paper-scissors")
    public String rockPaperScissors(Model model, HttpSession session) {
        User user = getUserFromSession(session);
        model.addAttribute("user", user);
        return "rock-paper-scissors";
    }

    @GetMapping("/games/snake-and-ladder")
    public String snakeAndLadder(Model model, HttpSession session) {
        model.addAttribute("user", getUserFromSession(session));
        return "snake-and-ladder";
    }

    @GetMapping("/play-chess")
    public String playChess(Model model, HttpSession session) {
        model.addAttribute("user", getUserFromSession(session));
        return "chess-game";
    }

    @GetMapping("/play-uno")
    public String playUno(Model model, HttpSession session) {
        User user = getUserFromSession(session);
        model.addAttribute("user", user);
        return "uno";
    }

    @GetMapping("/play-ludo")
    public String playLudo(Model model, HttpSession session) {
        User user = getUserFromSession(session);
        model.addAttribute("user", user);
        return "ludo";
    }

    @GetMapping("/play-mario")
    public String playMario() {
        return "mario";
    }

    @GetMapping("/play-bubble-shooter")
    public String playBubbleShooter() {
        return "bubble-shooter";
    }

    @GetMapping("/play-candy-crush")
    public String playCandyCrush() {
        return "candy-crush";
    }

    @GetMapping("/play-memory")
    public String playMemory() {
        return "memory-game";
    }

    @GetMapping("/play-runner")
    @Transactional
    public String playRunner(Model model, HttpSession session, HttpServletRequest request) {
        validateSessionOnPublicPage(session, request);
        model.addAttribute("user", getUserFromSession(session));
        return "runner";
    }

    @GetMapping("/play-car-game")
    public String playCarGame() {
        return "car-game";

    }

    @Transactional
    @GetMapping("/dashboard")
    public String dashboard(
            Model model,
            HttpSession session,
            @RequestParam(required = false) String category
    ) {
        Object sessionUser = session.getAttribute("user");
        // Admin should land on admin dashboard, not student dashboard
        if ("admin".equals(sessionUser))
            return "redirect:/admin";

        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        // Always refresh and ensure it's in session
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        populateDashboardCommonModel(model, user);

        List<PostCollaboration> pendingRequests = postCollaborationRepository
                .findByUserAndStatus(user, CollaborationStatus.PENDING);
        model.addAttribute("user", user);
        model.addAttribute("pendingCount", pendingRequests.size());

        String normalizedCategory = (category == null) ? null : category.trim();
        if (normalizedCategory != null && normalizedCategory.isEmpty()) normalizedCategory = null;
        if (normalizedCategory != null && "ALL".equalsIgnoreCase(normalizedCategory)) normalizedCategory = null;

        // Personalized feed (based on VIEW/LIKE/SAVE/SHARE) with optional category filter
        List<Post> personalized = feedAlgorithmService.getPersonalizedFeed(user.getId(), 0, 200);
        if (normalizedCategory != null) {
            final String catKey = normalizedCategory.trim().toUpperCase();
            personalized = personalized.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().trim().equalsIgnoreCase(catKey))
                    .toList();
        }
        model.addAttribute("posts", personalized);
        model.addAttribute("activeCategory", normalizedCategory);
        model.addAttribute("isReelsPage", false);

        List<User> allUsers = userRepository.findAll();
        final User finalUser = user;
        Set<User> following = user.getFollowing();
        List<User> suggestions = allUsers.stream()
                .filter(u -> !u.getId().equals(finalUser.getId()))
                .filter(u -> !following.contains(u))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("suggestions", suggestions);

        return "dashboard";
    }

    @Transactional
    @GetMapping("/reels")
    public String reels(Model model, HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if ("admin".equals(sessionUser))
            return "redirect:/admin";

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        populateDashboardCommonModel(model, user);

        List<PostCollaboration> pendingRequests = postCollaborationRepository
                .findByUserAndStatus(user, CollaborationStatus.PENDING);
        model.addAttribute("user", user);
        model.addAttribute("pendingCount", pendingRequests.size());

        List<Post> reels = new java.util.ArrayList<>(postRepository.findByPostTypeOrderByCreatedAtDesc("REEL"));
        reels.removeIf(p -> p.isBlocked() || "BANNED".equals(p.getUser().getStatus()) || "SUSPENDED".equals(p.getUser().getStatus()));
        model.addAttribute("posts", reels);
        model.addAttribute("activeCategory", null);
        model.addAttribute("isReelsPage", true);

        return "dashboard";
    }

    private String populateDashboardCommonModel(Model model, User user) {
        List<User> allUsers = userRepository.findAll();
        final User finalUser = user;
        Set<User> following = user.getFollowing();
        List<User> suggestions = allUsers.stream()
                .filter(u -> !u.getId().equals(finalUser.getId()))
                .filter(u -> !following.contains(u))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("suggestions", suggestions);

        Set<Long> requestedUserIds = followRequestRepository.findAll().stream()
                .filter(r -> r.getSender().getId().equals(finalUser.getId()))
                .map(r -> r.getReceiver().getId())
                .collect(Collectors.toSet());
        model.addAttribute("requestedUserIds", requestedUserIds);

        Set<Long> followingUserIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        model.addAttribute("followingUserIds", followingUserIds);

        model.addAttribute("notifications", notificationRepository.findByUserOrderByCreatedAtDesc(user));
        model.addAttribute("unreadNotifCount", notificationRepository.countByUserAndIsRead(user, false));

        // Check if current user has a story
        boolean hasStory = !postRepository.findByUserAndPostTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                user, "STORY", java.time.LocalDateTime.now().minusHours(24)).isEmpty();
        model.addAttribute("hasStory", hasStory);

        // Find all followed users with active stories
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusHours(24);
        List<User> storyUsers = new java.util.ArrayList<>();
        for (User otherUser : user.getFollowing()) {
            boolean fHasStory = !postRepository.findByUserAndPostTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                    otherUser, "STORY", cutoff).isEmpty();
            if (fHasStory) {
                storyUsers.add(otherUser);
            }
        }
        model.addAttribute("storyUsers", storyUsers);

        // Fetch active voting polls
        List<Event> votingPolls = eventRepository.findAll().stream()
                .filter(e -> "VOTING".equals(e.getStatus()))
                .filter(e -> e.getVotingEndDate() == null || e.getVotingEndDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        model.addAttribute("votingPolls", votingPolls);

        java.util.Set<Long> votedPollIds = new java.util.HashSet<>();
        if (user != null) {
            java.util.List<com.example.demo.model.Vote> userVotes = voteRepository.findByUserId(user.getId());
            votedPollIds = userVotes.stream().map(com.example.demo.model.Vote::getPollId).collect(Collectors.toSet());
        }
        model.addAttribute("votedPollIds", votedPollIds);

        // Ongoing music battles (rooms)
        List<MusicRoom> ongoingBattles = musicRoomRepository.findTop5ByActiveTrueAndPhaseNotOrderByCreatedAtDesc("ENDED");
        model.addAttribute("ongoingMusicBattles", ongoingBattles);

        return "dashboard";
    }

    @GetMapping("/admin")
    @Transactional
    public String admin(Model model, HttpSession session) {
        // Only let admin session through
        if (!"admin".equals(session.getAttribute("user")))
            return "redirect:/login";
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("events", eventRepository.findAll());
        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("upcomingEvents", eventRepository.countByStatus("UPCOMING"));
        model.addAttribute("ongoingEvents", eventRepository.countByStatus("ONGOING"));
        model.addAttribute("votingCount", eventRepository.countByStatus("VOTING"));
        model.addAttribute("completedCount", eventRepository.countByStatus("COMPLETED"));
        model.addAttribute("rewardConfig", rewardService.getConfig());
        model.addAttribute("contactMessages", contactMessageRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("posts", postRepository.findAllByOrderByCreatedAtDesc());
        
        // Battle details for admin overview
        List<Battle> battles = battleRepository.findAllByOrderByCreatedAtDesc();
        long runningBattles = battles.stream().filter(b -> "ACTIVE".equals(b.getStatus())).count();
        long totalJoinedUsers = battleParticipantRepository.count();
        
        double totalAdminCommission = 0.0;
        for (Battle b : battles) {
            double entryFee = b.getEntryFee() != null ? b.getEntryFee() : 0.0;
            // Creator auto-joins and is stored in b.participants, so other joining users are (participants size - 1)
            int joinsCount = b.getParticipants() != null ? Math.max(0, b.getParticipants().size() - 1) : 0;
            totalAdminCommission += entryFee * 0.07 * joinsCount;
        }

        model.addAttribute("battles", battles);
        model.addAttribute("totalBattles", battles.size());
        model.addAttribute("runningBattles", runningBattles);
        model.addAttribute("totalJoinedUsers", totalJoinedUsers);
        model.addAttribute("totalAdminCommission", totalAdminCommission);

        User user = getUserFromSession(session);
        model.addAttribute("user", user);
        return "admin-dashboard";
    }

    @GetMapping("/admin/battles")
    @Transactional
    public String adminBattles(Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("user")))
            return "redirect:/login";

        List<Battle> battles = battleRepository.findAllByOrderByCreatedAtDesc();
        long runningBattles = battles.stream().filter(b -> "ACTIVE".equals(b.getStatus())).count();
        long totalJoinedUsers = battleParticipantRepository.count();
        
        double totalAdminCommission = 0.0;
        for (Battle b : battles) {
            double entryFee = b.getEntryFee() != null ? b.getEntryFee() : 0.0;
            int joinsCount = b.getParticipants() != null ? Math.max(0, b.getParticipants().size() - 1) : 0;
            totalAdminCommission += entryFee * 0.07 * joinsCount;
        }

        model.addAttribute("battles", battles);
        model.addAttribute("totalBattles", battles.size());
        model.addAttribute("runningBattles", runningBattles);
        model.addAttribute("totalJoinedUsers", totalJoinedUsers);
        model.addAttribute("totalAdminCommission", totalAdminCommission);

        User user = getUserFromSession(session);
        model.addAttribute("user", user);
        return "admin-battles";
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/users/{id}/status")
    @Transactional
    public String updateUserStatus(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam String status,
            HttpSession session
    ) {
        if (!"admin".equals(session.getAttribute("user")))
            return "redirect:/login";

        User targetUser = userRepository.findById(id).orElse(null);
        if (targetUser != null) {
            targetUser.setStatus(status);
            userRepository.save(targetUser);
            feedAlgorithmService.evictFeedCache();
        }
        return "redirect:/admin#users-section";
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/posts/{id}/block")
    @Transactional
    public String updatePostBlockStatus(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam boolean blocked,
            HttpSession session
    ) {
        if (!"admin".equals(session.getAttribute("user")))
            return "redirect:/login";

        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            post.setBlocked(blocked);
            postRepository.save(post);
            feedAlgorithmService.evictFeedCache();
        }
        return "redirect:/admin#posts-section";
    }

    // ── Stub routes: sidebar links that don't have full pages yet ──

    @GetMapping("/explore")
    public String explore(Model model, HttpSession session) {
        if (!isLoggedIn(session))
            return "redirect:/login";

        User currentUser = getUserFromSession(session);
        if (currentUser == null)
            return "redirect:/login";
        final User finalUser = userRepository.findById(currentUser.getId()).orElse(currentUser);

        // All users except self
        List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(finalUser.getId()))
                .collect(Collectors.toList());

        Set<Long> followingUserIds = finalUser.getFollowing().stream()
                .map(User::getId).collect(Collectors.toSet());

        final Long currentUserId = finalUser.getId();
        Set<Long> requestedUserIds = followRequestRepository.findAll().stream()
                .filter(r -> r.getSender().getId().equals(currentUserId))
                .map(r -> r.getReceiver().getId())
                .collect(Collectors.toSet());

        // Follower counts map: userId -> count
        java.util.Map<Long, Integer> followersCount = new java.util.HashMap<>();
        for (User u : allUsers) {
            followersCount.put(u.getId(), u.getFollowers().size());
        }

        model.addAttribute("user", finalUser);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("followingUserIds", followingUserIds);
        model.addAttribute("requestedUserIds", requestedUserIds);
        model.addAttribute("followersCount", followersCount);
        model.addAttribute("totalPeopleCount", allUsers.size());

        // Pending follow requests for current user
        List<FollowRequest> followRequests = followRequestRepository.findAll().stream()
                .filter(r -> r.getReceiver().getId().equals(finalUser.getId()))
                .collect(Collectors.toList());
        model.addAttribute("followRequests", followRequests);

        return "explore";
    }

    @GetMapping("/achievements")
    public String achievements(Model model, HttpSession session) {
        if (!isLoggedIn(session))
            return "redirect:/login";

        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            User u = (User) sessionUser;
            User dbUser = userRepository.findById(u.getId()).orElse(u);
            model.addAttribute("user", dbUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("attendedCount",
                    eventRegistrationRepository.countByUserAndAttendanceMarked(dbUser, true));
            // Fetch all COMPLETED event registrations with a position
            List<EventRegistration> userAchievements = eventRegistrationRepository.findByUser(dbUser).stream()
                    .filter(r -> r.getPosition() != null && !"Participant".equals(r.getPosition())
                            && !"Absent".equals(r.getPosition()))
                    .collect(Collectors.toList());
            model.addAttribute("achievements", userAchievements);
        } else {
            model.addAttribute("user", null);
            model.addAttribute("isAdmin", true);
            model.addAttribute("achievements", List.of());
        }

        List<User> leaderboard = userRepository.findAll();
        leaderboard.sort((u1, u2) -> {
            int xp1 = u1.getXp() != null ? u1.getXp() : 0;
            int xp2 = u2.getXp() != null ? u2.getXp() : 0;
            return Integer.compare(xp2, xp1);
        });
        model.addAttribute("leaderboard", leaderboard);

        return "achievements";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session) {
        if (!isLoggedIn(session))
            return "redirect:/login";
        return "redirect:/dashboard";
    }

    /** True if any valid session (student or admin) exists */
    private boolean isLoggedIn(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser != null) return true;
        
        Object u = session.getAttribute("user");
        return u instanceof User || "admin".equals(u);
    }
}
