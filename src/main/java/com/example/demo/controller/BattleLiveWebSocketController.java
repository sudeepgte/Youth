package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Controller
public class BattleLiveWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private BattleRepository battleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BattleLiveCommentRepository commentRepository;
    @Autowired
    private BattleLikeRepository likeRepository;
    @Autowired
    private BattleGiftRepository giftRepository;
    @Autowired
    private BattleVoteRepository voteRepository;
    @Autowired
    private BattleSubmissionRepository submissionRepository;
    @Autowired
    private BattleParticipantRepository participantRepository;
    @Autowired
    private WalletService walletService;
    @Autowired
    private com.example.demo.service.BattleDisconnectService battleDisconnectService;
    @Autowired
    private com.example.demo.service.VoteProtectionService voteProtectionService;
    @Autowired
    private com.example.demo.service.NotificationService notificationService;
    @Autowired
    private com.example.demo.service.AuditLogService auditLogService;

    // Track online viewers
    private final ConcurrentHashMap<Long, Set<Long>> battleViewers = new ConcurrentHashMap<>();
    
    // Chat Rate Limiting: user id -> deque of timestamps
    private final ConcurrentHashMap<Long, Deque<Long>> chatTimestamps = new ConcurrentHashMap<>();
    
    // Spam protection: user id -> last message text & timestamp
    private final ConcurrentHashMap<Long, String> lastMessageText = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> lastMessageTime = new ConcurrentHashMap<>();

    // Moderation state per battle
    private final ConcurrentHashMap<Long, Set<Long>> mutedUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<Long>> blockedUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> battleSlowMode = new ConcurrentHashMap<>(); // delay in seconds

    // Simple profanity list
    private static final List<String> BAD_WORDS = Arrays.asList("fuck", "shit", "bitch", "asshole", "cunt", "dick", "bastard");

    private String filterProfanity(String text) {
        if (text == null) return null;
        String filtered = text;
        for (String word : BAD_WORDS) {
            filtered = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b").matcher(filtered).replaceAll("***");
        }
        return filtered;
    }

    private boolean isAdminOrCreator(User user, Battle battle) {
        // Checking if user is battle creator
        return battle.getCreator().getId().equals(user.getId()); 
    }

    @MessageMapping("/battle/{battleId}/signal")
    public void handleSignal(@DestinationVariable Long battleId, @Payload Map<String, Object> signal, Principal principal) {
        Object targetUserIdObj = signal.get("targetUserId");
        if (targetUserIdObj != null) {
            String targetUserId = targetUserIdObj.toString();
            messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/signal/" + targetUserId, (Object) signal);
        } else {
            messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/signal", (Object) signal);
        }
    }

    @MessageMapping("/battle/{battleId}/comment")
    public void handleComment(@DestinationVariable Long battleId, @Payload Map<String, String> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        
        // Check blocks and mutes
        Set<Long> blocked = blockedUsers.getOrDefault(battleId, Collections.emptySet());
        if (blocked.contains(userId)) return; // Blocked completely from interaction

        Set<Long> muted = mutedUsers.getOrDefault(battleId, Collections.emptySet());
        if (muted.contains(userId)) {
            // Send personal notification to user that they are muted
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/battle/" + battleId + "/notifications", 
                Collections.singletonMap("error", "You have been muted in this chat."));
            return;
        }

        long now = System.currentTimeMillis();

        // Slow Mode check
        int slowModeDelaySeconds = battleSlowMode.getOrDefault(battleId, 0);
        if (slowModeDelaySeconds > 0) {
            Long lastSent = lastMessageTime.get(userId);
            if (lastSent != null && now - lastSent < (slowModeDelaySeconds * 1000L)) {
                // Too fast for slow mode
                return;
            }
        }

        // --- Rate Limiting: Max 5 messages per 10 seconds ---
        Deque<Long> stamps = chatTimestamps.computeIfAbsent(userId, k -> new LinkedList<>());
        synchronized (stamps) {
            while (!stamps.isEmpty() && now - stamps.peekFirst() > 10000) {
                stamps.pollFirst();
            }
            if (stamps.size() >= 5) {
                // Rate limit exceeded
                return;
            }
            stamps.addLast(now);
        }
        
        User user = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (user == null || battle == null) return;

        String message = payload.get("message");
        if (message == null || message.trim().isEmpty()) return;
        message = message.trim();

        // Spam protection (duplicate messages within 15 seconds)
        Long lastTime = lastMessageTime.get(userId);
        String lastMsg = lastMessageText.get(userId);
        if (lastTime != null && lastMsg != null && lastMsg.equalsIgnoreCase(message) && (now - lastTime < 15000)) {
            return; // Duplicate spam blocked
        }

        if (message.length() > 500) message = message.substring(0, 500);

        // Profanity Filter
        message = filterProfanity(message);

        // Update spam trackers
        lastMessageText.put(userId, message);
        lastMessageTime.put(userId, now);

        BattleLiveComment comment = new BattleLiveComment();
        comment.setBattle(battle);
        comment.setUser(user);
        comment.setMessage(message);
        comment = commentRepository.save(comment);

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("id", comment.getId());
        broadcast.put("userId", user.getId());
        broadcast.put("username", user.getUsername());
        broadcast.put("profilePhotoUrl", user.getProfilePhotoUrl());
        broadcast.put("message", message);
        broadcast.put("sentAt", comment.getSentAt().toString());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/comments", (Object) broadcast);
    }

    // Moderation: Delete a comment
    @MessageMapping("/battle/{battleId}/comment/delete")
    public void handleDeleteComment(@DestinationVariable Long battleId, @Payload Map<String, Long> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        User user = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        
        if (user == null || battle == null || !isAdminOrCreator(user, battle)) return; // Admin/creator only

        // Handle possible casting issues since JS might send as Integer depending on size
        Object commentIdObj = payload.get("commentId");
        if (commentIdObj == null) return;
        Long commentId = Long.parseLong(commentIdObj.toString());

        BattleLiveComment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null && comment.getBattle().getId().equals(battleId)) {
            comment.setDeleted(true);
            commentRepository.save(comment);

            // Broadcast deletion so UI removes it
            Map<String, Object> broadcast = new HashMap<>();
            broadcast.put("action", "delete");
            broadcast.put("commentId", commentId);
            messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/moderation", (Object) broadcast);
            
            auditLogService.log("MODERATION_DELETE", userId, "Deleted comment " + commentId + " in battle " + battleId);
        }
    }

    // Moderation: Report a comment
    @MessageMapping("/battle/{battleId}/comment/report")
    public void handleReportComment(@DestinationVariable Long battleId, @Payload Map<String, Long> payload, Principal principal) {
        Object commentIdObj = payload.get("commentId");
        if (commentIdObj == null) return;
        Long commentId = Long.parseLong(commentIdObj.toString());

        BattleLiveComment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null && comment.getBattle().getId().equals(battleId)) {
            comment.setReported(true);
            commentRepository.save(comment);
            
            // Send acknowledgement to the user who reported
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/battle/" + battleId + "/notifications", 
                Collections.singletonMap("message", "Comment reported successfully."));
                
            auditLogService.log("MODERATION_REPORT", Long.parseLong(principal.getName()), "Reported comment " + commentId + " in battle " + battleId);
        }
    }

    // Moderation: Mute a user
    @MessageMapping("/battle/{battleId}/mute")
    public void handleMuteUser(@DestinationVariable Long battleId, @Payload Map<String, Long> payload, Principal principal) {
        Long adminId = Long.parseLong(principal.getName());
        User admin = userRepository.findById(adminId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        
        if (admin == null || battle == null || !isAdminOrCreator(admin, battle)) return; 

        Object targetUserIdObj = payload.get("userId");
        if (targetUserIdObj == null) return;
        Long targetUserId = Long.parseLong(targetUserIdObj.toString());

        mutedUsers.computeIfAbsent(battleId, k -> ConcurrentHashMap.newKeySet()).add(targetUserId);

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("action", "mute");
        broadcast.put("userId", targetUserId);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/moderation", (Object) broadcast);
        
        auditLogService.log("MODERATION_MUTE", adminId, "Muted user " + targetUserId + " in battle " + battleId);
    }

    // Moderation: Block a user
    @MessageMapping("/battle/{battleId}/block")
    public void handleBlockUser(@DestinationVariable Long battleId, @Payload Map<String, Long> payload, Principal principal) {
        Long adminId = Long.parseLong(principal.getName());
        User admin = userRepository.findById(adminId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        
        if (admin == null || battle == null || !isAdminOrCreator(admin, battle)) return; 

        Object targetUserIdObj = payload.get("userId");
        if (targetUserIdObj == null) return;
        Long targetUserId = Long.parseLong(targetUserIdObj.toString());

        blockedUsers.computeIfAbsent(battleId, k -> ConcurrentHashMap.newKeySet()).add(targetUserId);

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("action", "block");
        broadcast.put("userId", targetUserId);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/moderation", (Object) broadcast);
        
        auditLogService.log("MODERATION_BLOCK", adminId, "Blocked user " + targetUserId + " in battle " + battleId);
    }

    // Moderation: Set Slow Mode
    @MessageMapping("/battle/{battleId}/slowmode")
    public void handleSlowMode(@DestinationVariable Long battleId, @Payload Map<String, Integer> payload, Principal principal) {
        Long adminId = Long.parseLong(principal.getName());
        User admin = userRepository.findById(adminId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        
        if (admin == null || battle == null || !isAdminOrCreator(admin, battle)) return; 

        Integer delaySeconds = payload.getOrDefault("delay", 0);
        battleSlowMode.put(battleId, delaySeconds);

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("action", "slowmode");
        broadcast.put("delaySeconds", delaySeconds);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/moderation", (Object) broadcast);
    }
    
    @MessageMapping("/battle/{battleId}/reaction")
    public void handleReaction(@DestinationVariable Long battleId, @Payload Map<String, String> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Set<Long> blocked = blockedUsers.getOrDefault(battleId, Collections.emptySet());
        if (blocked.contains(userId)) return;

        String reaction = payload.get("reaction"); // e.g. "heart", "fire"
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("reaction", reaction);
        broadcast.put("senderId", principal.getName());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/reactions", (Object) broadcast);
    }

    @MessageMapping("/battle/{battleId}/like")
    public void handleLike(@DestinationVariable Long battleId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Set<Long> blocked = blockedUsers.getOrDefault(battleId, Collections.emptySet());
        if (blocked.contains(userId)) return;

        User user = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (user == null || battle == null) return;

        if (!likeRepository.existsByBattleAndUser(battle, user)) {
            BattleLike like = new BattleLike();
            like.setBattle(battle);
            like.setUser(user);
            likeRepository.save(like);

            long count = likeRepository.countByBattle(battle);
            battle.setLikeCount((int) count);
            battleRepository.save(battle);
        }
        
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("totalLikes", likeRepository.countByBattle(battle));
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/likes", (Object) broadcast);
    }

    @MessageMapping("/battle/{battleId}/gift")
    public void handleGift(@DestinationVariable Long battleId, @Payload Map<String, Object> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Set<Long> blocked = blockedUsers.getOrDefault(battleId, Collections.emptySet());
        if (blocked.contains(userId)) return;

        User sender = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (sender == null || battle == null) return;

        String giftType = (String) payload.get("giftType");
        Long recipientId = Long.parseLong(payload.get("recipientId").toString());
        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) return;

        Map<String, Integer> giftCosts = Map.of(
            "ROSE", 10, "FIRE", 20, "GIFT_BOX", 50, "DIAMOND", 100, "CROWN", 200
        );
        int cost = giftCosts.getOrDefault(giftType, 10);

        // Transactional Debit
        boolean success = walletService.processTransaction(sender, (double) cost, "COINS", "DEBIT", 
                "Gifted " + giftType + " to " + recipient.getUsername(), "GIFT_" + battleId);
        
        if (!success) return; // Insufficient coins

        // Save gift record
        BattleGift gift = new BattleGift();
        gift.setBattle(battle);
        gift.setSender(sender);
        gift.setRecipient(recipient);
        gift.setGiftType(giftType);
        gift.setCoinsCost(cost);
        giftRepository.save(gift);

        long giftCount = giftRepository.countByBattle(battle);
        battle.setGiftCount((int) giftCount);
        battleRepository.save(battle);
        
        auditLogService.log("GIFT", sender.getId(), "Gifted " + giftType + " to participant " + recipientId + " in battle " + battleId + " (Cost: " + cost + ")");

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("senderId", sender.getId());
        broadcast.put("senderUsername", sender.getUsername());
        broadcast.put("recipientUsername", recipient.getUsername());
        broadcast.put("giftType", giftType);
        broadcast.put("coinsCost", cost);
        broadcast.put("totalGifts", giftCount);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/gifts", (Object) broadcast);
        
        notificationService.sendNotification(recipient.getId(), "Gift Received", sender.getUsername() + " sent you a " + giftType + "!", "fas fa-gift", "/battles/" + battleId);
    }
    
    @MessageMapping("/battle/{battleId}/rematch")
    public void handleRematch(@DestinationVariable Long battleId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        User sender = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (sender == null || battle == null) return;
        
        // Find the opponent
        List<BattleParticipant> participants = participantRepository.findByBattle(battle);
        for (BattleParticipant bp : participants) {
            if (!bp.getUser().getId().equals(userId)) {
                notificationService.sendNotification(bp.getUser().getId(), "Rematch Request", sender.getUsername() + " wants a rematch!", "fas fa-redo", "/battles/create"); // Or wherever rematch leads
                
                // Also broadcast to the battle chat/status so UI can show it if they are still on the page
                Map<String, Object> broadcast = new HashMap<>();
                broadcast.put("type", "REMATCH_REQUEST");
                broadcast.put("senderId", userId);
                broadcast.put("senderUsername", sender.getUsername());
                messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/status", (Object) broadcast);
            }
        }
    }

    @MessageMapping("/battle/{battleId}/live-vote")
    public void handleVote(@DestinationVariable Long battleId, @Payload Map<String, Object> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Set<Long> blocked = blockedUsers.getOrDefault(battleId, Collections.emptySet());
        if (blocked.contains(userId)) return;

        User voter = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (voter == null || battle == null) return;
        if (!"ACTIVE".equals(battle.getStatus()) && !"VOTING".equals(battle.getStatus())) return;
        
        Long participantUserId = Long.parseLong(payload.get("participantUserId").toString());
        
        if (!voteProtectionService.isVoteAllowed(battleId, voter, participantUserId)) return;
        
        if (voteRepository.existsByBattleAndVoter(battle, voter)) return;
        User participant = userRepository.findById(participantUserId).orElse(null);
        if (participant == null) return;

        java.util.Optional<BattleSubmission> subOpt = submissionRepository.findByBattleAndUser(battle, participant);
        BattleSubmission sub;
        if (subOpt.isPresent()) {
            sub = subOpt.get();
        } else {
            sub = new BattleSubmission();
            sub.setBattle(battle);
            sub.setUser(participant);
            sub.setSubmissionUrl("Live Vote");
            sub = submissionRepository.save(sub);
        }

        BattleVote vote = new BattleVote();
        vote.setBattle(battle);
        vote.setSubmission(sub);
        vote.setVoter(voter);
        voteRepository.save(vote);

        voteProtectionService.recordVote(battleId, voter.getId());

        sub.setVoteCount(sub.getVoteCount() + 1);
        submissionRepository.save(sub);
        
        auditLogService.log("VOTE", voter.getId(), "Voted for participant " + participantUserId + " in battle " + battleId);

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("participantId", participantUserId);
        broadcast.put("totalVotes", sub.getVoteCount());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/votes", (Object) broadcast);
    }

    public int getViewerCount(Long battleId) { Set<Long> viewers = battleViewers.get(battleId); return viewers != null ? viewers.size() : 0; }

    @MessageMapping("/battle/{battleId}/viewer-join")
    public void handleViewerJoin(@DestinationVariable Long battleId, Principal principal, org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null) {
            Long userId = Long.parseLong(principal.getName());
            battleViewers.computeIfAbsent(battleId, k -> ConcurrentHashMap.newKeySet()).add(userId);
            broadcastViewerCount(battleId);
            if (battleDisconnectService != null) {
                battleDisconnectService.registerSession(headerAccessor.getSessionId(), userId, battleId);
            }
        }
    }

    @MessageMapping("/battle/{battleId}/viewer-leave")
    public void handleViewerLeave(@DestinationVariable Long battleId, Principal principal) {
        if (principal != null) {
            Long userId = Long.parseLong(principal.getName());
            Set<Long> viewers = battleViewers.get(battleId);
            if (viewers != null) {
                viewers.remove(userId);
                broadcastViewerCount(battleId);
            }
        }
    }

    private void broadcastViewerCount(Long battleId) {
        Set<Long> viewers = battleViewers.getOrDefault(battleId, Collections.emptySet());
        Map<String, Object> data = new HashMap<>();
        data.put("viewerCount", viewers.size());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/status", (Object) data);
    }
}


