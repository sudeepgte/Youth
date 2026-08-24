package com.example.demo.service;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleParticipant;
import com.example.demo.model.User;
import com.example.demo.repository.BattleParticipantRepository;
import com.example.demo.repository.BattleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class MatchmakingService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    // A thread-safe list representing the matchmaking queue
    private final List<MatchRequest> queue = new CopyOnWriteArrayList<>();

    // Map to hold pending lobbies before they officially start
    private final Map<Long, BattleLobby> activeLobbies = new ConcurrentHashMap<>();

    public static class MatchRequest {
        public Long userId;
        public String category;
        public String skillLevel;
        public Integer durationMinutes;
        public Integer elo;
        public LocalDateTime requestTime;

        public MatchRequest(Long userId, String category, String skillLevel, Integer durationMinutes, Integer elo) {
            this.userId = userId;
            this.category = category;
            this.skillLevel = skillLevel;
            this.durationMinutes = durationMinutes;
            this.elo = elo;
            this.requestTime = LocalDateTime.now();
        }
    }

    public static class BattleLobby {
        public Long battleId;
        public Long player1Id;
        public Long player2Id;
        public boolean p1Ready = false;
        public boolean p2Ready = false;
        public LocalDateTime createdAt = LocalDateTime.now();
    }

    public void joinQueue(Long userId, String category, String skillLevel, Integer duration) {
        // Remove if already in queue to prevent duplicates
        leaveQueue(userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        MatchRequest req = new MatchRequest(userId, category, skillLevel, duration, user.getBattleRating());
        queue.add(req);
        
        // Notify user they joined successfully
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "QUEUE_JOINED");
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/matchmaking", msg);
    }

    public void leaveQueue(Long userId) {
        queue.removeIf(req -> req.userId.equals(userId));
    }

    // Runs every 2 seconds to find matches
    @Scheduled(fixedRate = 2000)
    public void processQueue() {
        if (queue.size() < 2) return;

        List<MatchRequest> unmatched = new ArrayList<>(queue);

        while (unmatched.size() >= 2) {
            MatchRequest p1 = unmatched.remove(0);
            MatchRequest matchedP2 = null;

            for (MatchRequest p2 : unmatched) {
                // Must not be the same user
                if (p1.userId.equals(p2.userId)) continue;
                
                // Must match category, skill, duration
                if (!p1.category.equals(p2.category)) continue;
                if (!p1.skillLevel.equals(p2.skillLevel)) continue;
                if (!p1.durationMinutes.equals(p2.durationMinutes)) continue;

                // ELO check (Expanding search radius over time could be added here, currently fixed 150)
                if (Math.abs(p1.elo - p2.elo) <= 150) {
                    matchedP2 = p2;
                    break;
                }
            }

            if (matchedP2 != null) {
                unmatched.remove(matchedP2);
                queue.remove(p1);
                queue.remove(matchedP2);
                
                createMatch(p1, matchedP2);
            }
        }
    }

    private void createMatch(MatchRequest p1, MatchRequest p2) {
        User u1 = userRepository.findById(p1.userId).orElse(null);
        User u2 = userRepository.findById(p2.userId).orElse(null);
        if (u1 == null || u2 == null) return;

        // 1. Create a new Battle in LOBBY status
        Battle battle = new Battle();
        battle.setTitle("Quick Battle: " + u1.getUsername() + " vs " + u2.getUsername());
        battle.setCategory(p1.category);
        battle.setDifficulty(p1.skillLevel);
        battle.setDurationMinutes(p1.durationMinutes);
        battle.setMode("ONLINE");
        battle.setBattleType("QUICK");
        battle.setStatus("LOBBY");
        battle.setRoomCode(generateRoomCode());
        battle.setCreatedAt(LocalDateTime.now());
        
        battle = battleRepository.save(battle);

        // 2. Add Participants
        BattleParticipant bp1 = new BattleParticipant();
        bp1.setBattle(battle);
        bp1.setUser(u1);
        participantRepository.save(bp1);

        BattleParticipant bp2 = new BattleParticipant();
        bp2.setBattle(battle);
        bp2.setUser(u2);
        participantRepository.save(bp2);

        // 3. Register Lobby State
        BattleLobby lobby = new BattleLobby();
        lobby.battleId = battle.getId();
        lobby.player1Id = p1.userId;
        lobby.player2Id = p2.userId;
        activeLobbies.put(battle.getId(), lobby);

        // 4. Broadcast Match Found to both users
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "MATCH_FOUND");
        msg.put("battleId", battle.getId());
        msg.put("roomCode", battle.getRoomCode());
        
        Map<String, Object> opponentForP1 = new HashMap<>();
        opponentForP1.put("id", u2.getId());
        opponentForP1.put("username", u2.getUsername());
        opponentForP1.put("rating", u2.getBattleRating());
        opponentForP1.put("photo", u2.getProfilePhotoUrl());
        
        Map<String, Object> opponentForP2 = new HashMap<>();
        opponentForP2.put("id", u1.getId());
        opponentForP2.put("username", u1.getUsername());
        opponentForP2.put("rating", u1.getBattleRating());
        opponentForP2.put("photo", u1.getProfilePhotoUrl());

        Map<String, Object> p1Msg = new HashMap<>(msg);
        p1Msg.put("opponent", opponentForP1);
        messagingTemplate.convertAndSendToUser(p1.userId.toString(), "/queue/matchmaking", p1Msg);

        Map<String, Object> p2Msg = new HashMap<>(msg);
        p2Msg.put("opponent", opponentForP2);
        messagingTemplate.convertAndSendToUser(p2.userId.toString(), "/queue/matchmaking", p2Msg);
    }

    public void setPlayerReady(Long battleId, Long userId) {
        BattleLobby lobby = activeLobbies.get(battleId);
        if (lobby == null) return;

        if (lobby.player1Id.equals(userId)) lobby.p1Ready = true;
        if (lobby.player2Id.equals(userId)) lobby.p2Ready = true;

        // Broadcast ready state
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "PLAYER_READY");
        msg.put("userId", userId);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId, (Object) msg);

        // If both ready, start countdown
        if (lobby.p1Ready && lobby.p2Ready) {
            startCountdown(battleId);
        }
    }

    private void startCountdown(Long battleId) {
        new Thread(() -> {
            try {
                for (int i = 10; i > 0; i--) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("type", "COUNTDOWN");
                    msg.put("seconds", i);
                    messagingTemplate.convertAndSend("/topic/battle/" + battleId, (Object) msg);
                    Thread.sleep(1000);
                }
                
                // Start battle officially
                Battle battle = battleRepository.findById(battleId).orElse(null);
                if (battle != null) {
                    battle.setStatus("ACTIVE");
                    battle.setStartedAt(LocalDateTime.now());
                    battle.setEndsAt(LocalDateTime.now().plusMinutes(battle.getDurationMinutes()));
                    battleRepository.save(battle);

                    Map<String, Object> startMsg = new HashMap<>();
                    startMsg.put("type", "BATTLE_START");
                    startMsg.put("endsAt", battle.getEndsAt().toString());
                    messagingTemplate.convertAndSend("/topic/battle/" + battleId, (Object) startMsg);
                }
                
                activeLobbies.remove(battleId);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private String generateRoomCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
