package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
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

    // Track online viewers per battle (in-memory)
    private final ConcurrentHashMap<Long, Set<Long>> battleViewers = new ConcurrentHashMap<>();

    // 1. WebRTC Signaling - just forward the message to the battle topic
    @MessageMapping("/battle/{battleId}/signal")
    public void handleSignal(@DestinationVariable Long battleId, @Payload Map<String, Object> signal, Principal principal) {
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/signal", (Object) signal);
    }

    // 2. Live Comment
    @MessageMapping("/battle/{battleId}/comment")
    public void handleComment(@DestinationVariable Long battleId, @Payload Map<String, String> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        User user = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (user == null || battle == null) return;

        String message = payload.get("message");
        if (message == null || message.trim().isEmpty()) return;
        if (message.length() > 500) message = message.substring(0, 500);

        // Persist
        BattleLiveComment comment = new BattleLiveComment();
        comment.setBattle(battle);
        comment.setUser(user);
        comment.setMessage(message.trim());
        commentRepository.save(comment);

        // Broadcast
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("username", user.getUsername());
        broadcast.put("profilePhotoUrl", user.getProfilePhotoUrl());
        broadcast.put("message", message.trim());
        broadcast.put("sentAt", comment.getSentAt().toString());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/comments", (Object) broadcast);
    }

    // 3. Like
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

        long totalLikes = likeRepository.countByBattle(battle);
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("likeCount", totalLikes);
        broadcast.put("username", user.getUsername());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/likes", (Object) broadcast);
    }

    // 4. Gift
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

        // Gift costs
        Map<String, Integer> giftCosts = Map.of(
            "ROSE", 10, "FIRE", 20, "GIFT_BOX", 50, "DIAMOND", 100, "CROWN", 200
        );
        int cost = giftCosts.getOrDefault(giftType, 10);

        // Check coins
        if (sender.getCoins() < cost) return;

        // Deduct coins from sender
        sender.setCoins(sender.getCoins() - cost);
        userRepository.save(sender);

        // Save gift
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

        // Broadcast
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("senderUsername", sender.getUsername());
        broadcast.put("recipientUsername", recipient.getUsername());
        broadcast.put("giftType", giftType);
        broadcast.put("coinsCost", cost);
        broadcast.put("totalGifts", giftCount);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/gifts", (Object) broadcast);
    }

    // 5. Vote (real-time)
    @MessageMapping("/battle/{battleId}/live-vote")
    public void handleVote(@DestinationVariable Long battleId, @Payload Map<String, Object> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        User voter = userRepository.findById(userId).orElse(null);
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (voter == null || battle == null) return;
        if (!"ACTIVE".equals(battle.getStatus()) && !"VOTING".equals(battle.getStatus())) return;
        if (voteRepository.existsByBattleAndVoter(battle, voter)) return;

        Long participantUserId = Long.parseLong(payload.get("participantUserId").toString());
        // Find or create submission for this participant
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
            sub.setSubmissionUrl("Live Battle");
            sub.setDescription("Live battle participant");
            submissionRepository.save(sub);
        }

        // Cast vote
        BattleVote vote = new BattleVote();
        vote.setBattle(battle);
        vote.setVoter(voter);
        vote.setSubmission(sub);
        voteRepository.save(vote);
        sub.setVoteCount(sub.getVoteCount() + 1);
        submissionRepository.save(sub);

        // Broadcast updated vote counts for all participants
        java.util.List<BattleSubmission> allSubs = submissionRepository.findByBattleOrderByVoteCountDesc(battle);
        java.util.List<Map<String, Object>> leaderboard = new java.util.ArrayList<>();
        for (BattleSubmission s : allSubs) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("userId", s.getUser().getId());
            entry.put("username", s.getUser().getUsername());
            entry.put("voteCount", s.getVoteCount());
            leaderboard.add(entry);
        }
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/votes", (Object) leaderboard);
    }

    // 6. Join (viewer count)
    @MessageMapping("/battle/{battleId}/viewer-join")
    public void handleViewerJoin(@DestinationVariable Long battleId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        battleViewers.computeIfAbsent(battleId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        broadcastViewerCount(battleId);
    }

    // 7. Leave (viewer count)
    @MessageMapping("/battle/{battleId}/viewer-leave")
    public void handleViewerLeave(@DestinationVariable Long battleId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Set<Long> viewers = battleViewers.get(battleId);
        if (viewers != null) viewers.remove(userId);
        broadcastViewerCount(battleId);
    }

    // 8. Media status (camera/mic toggle)
    @MessageMapping("/battle/{battleId}/media-status")
    public void handleMediaStatus(@DestinationVariable Long battleId, @Payload Map<String, Object> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        payload.put("userId", userId);
        payload.put("username", user.getUsername());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/participants", (Object) payload);
    }

    // 9. End Battle & Determine Winners
    @MessageMapping("/battle/{battleId}/end-battle")
    public void endBattle(@DestinationVariable Long battleId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (battle == null) return;
        
        // Only creator can end it manually/auto
        if (!battle.getCreator().getId().equals(userId)) return;
        
        if ("COMPLETED".equals(battle.getStatus())) return; // Already ended

        // Calculate winners based on votes
        java.util.List<BattleSubmission> subs = submissionRepository.findByBattleOrderByVoteCountDesc(battle);
        
        if (subs.size() > 0) {
            User firstPlace = subs.get(0).getUser();
            battle.setWinner(firstPlace);
            if (battle.getPrize1() != null && battle.getPrize1() > 0) {
                firstPlace.setWalletBalance((firstPlace.getWalletBalance() != null ? firstPlace.getWalletBalance() : 0.0) + battle.getPrize1());
                firstPlace.setXp((firstPlace.getXp() != null ? firstPlace.getXp() : 0) + (battle.getWinnerXp() != null ? battle.getWinnerXp() : 500));
                userRepository.save(firstPlace);
            }
        }
        if (subs.size() > 1) {
            User secondPlace = subs.get(1).getUser();
            battle.setWinner2(secondPlace);
            if (battle.getPrize2() != null && battle.getPrize2() > 0) {
                secondPlace.setWalletBalance((secondPlace.getWalletBalance() != null ? secondPlace.getWalletBalance() : 0.0) + battle.getPrize2());
                userRepository.save(secondPlace);
            }
        }
        if (subs.size() > 2) {
            User thirdPlace = subs.get(2).getUser();
            battle.setWinner3(thirdPlace);
            if (battle.getPrize3() != null && battle.getPrize3() > 0) {
                thirdPlace.setWalletBalance((thirdPlace.getWalletBalance() != null ? thirdPlace.getWalletBalance() : 0.0) + battle.getPrize3());
                userRepository.save(thirdPlace);
            }
        }

        battle.setStatus("COMPLETED");
        battle.setIsLive(false);
        battleRepository.save(battle);

        Map<String, Object> msg = new HashMap<>();
        msg.put("status", "COMPLETED");
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/status", (Object) msg);
    }

    private void broadcastViewerCount(Long battleId) {
        Set<Long> viewers = battleViewers.getOrDefault(battleId, java.util.Collections.emptySet());
        Map<String, Object> data = new HashMap<>();
        data.put("viewerCount", viewers.size());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/status", (Object) data);
    }
}
