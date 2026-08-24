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

    // Track online viewers
    private final ConcurrentHashMap<Long, Set<Long>> battleViewers = new ConcurrentHashMap<>();
    
    // Chat Rate Limiting: user id -> deque of timestamps
    private final ConcurrentHashMap<Long, Deque<Long>> chatTimestamps = new ConcurrentHashMap<>();

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
        
        // --- Rate Limiting: Max 5 messages per 10 seconds ---
        long now = System.currentTimeMillis();
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
        if (message.length() > 500) message = message.substring(0, 500);

        BattleLiveComment comment = new BattleLiveComment();
        comment.setBattle(battle);
        comment.setUser(user);
        comment.setMessage(message.trim());
        commentRepository.save(comment);

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("username", user.getUsername());
        broadcast.put("profilePhotoUrl", user.getProfilePhotoUrl());
        broadcast.put("message", message.trim());
        broadcast.put("sentAt", comment.getSentAt().toString());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/comments", (Object) broadcast);
    }
    
    @MessageMapping("/battle/{battleId}/reaction")
    public void handleReaction(@DestinationVariable Long battleId, @Payload Map<String, String> payload, Principal principal) {
        String reaction = payload.get("reaction"); // e.g. "heart", "fire"
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("reaction", reaction);
        broadcast.put("senderId", principal.getName());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/reactions", (Object) broadcast);
    }

    @MessageMapping("/battle/{battleId}/like")
    public void handleLike(@DestinationVariable Long battleId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
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

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("senderUsername", sender.getUsername());
        broadcast.put("recipientUsername", recipient.getUsername());
        broadcast.put("giftType", giftType);
        broadcast.put("coinsCost", cost);
        broadcast.put("totalGifts", giftCount);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/gifts", (Object) broadcast);
    }

    @MessageMapping("/battle/{battleId}/live-vote")
    public void handleVote(@DestinationVariable Long battleId, @Payload Map<String, Object> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        User voter = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (voter == null || battle == null) return;
        if (!"ACTIVE".equals(battle.getStatus()) && !"VOTING".equals(battle.getStatus())) return;
        if (voteRepository.existsByBattleAndVoter(battle, voter)) return;

        Long participantUserId = Long.parseLong(payload.get("participantUserId").toString());
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

        sub.setVoteCount(sub.getVoteCount() + 1);
        submissionRepository.save(sub);

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("participantId", participantUserId);
        broadcast.put("totalVotes", sub.getVoteCount());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/votes", (Object) broadcast);
    }

    @MessageMapping("/battle/{battleId}/viewer-join")
    public void handleViewerJoin(@DestinationVariable Long battleId, Principal principal) {
        if (principal != null) {
            Long userId = Long.parseLong(principal.getName());
            battleViewers.computeIfAbsent(battleId, k -> ConcurrentHashMap.newKeySet()).add(userId);
            broadcastViewerCount(battleId);
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