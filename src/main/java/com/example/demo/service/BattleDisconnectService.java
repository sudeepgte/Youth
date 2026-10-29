package com.example.demo.service;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleParticipant;
import com.example.demo.model.User;
import com.example.demo.repository.BattleParticipantRepository;
import com.example.demo.repository.BattleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class BattleDisconnectService {

    @Autowired private BattleRepository battleRepository;
    @Autowired private BattleParticipantRepository participantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletService walletService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // Maps sessionId -> userId
    private final ConcurrentHashMap<String, Long> sessionToUserMap = new ConcurrentHashMap<>();
    // Maps sessionId -> battleId
    private final ConcurrentHashMap<String, Long> sessionToBattleMap = new ConcurrentHashMap<>();
    
    @Autowired private BattleTimerService battleTimerService;
    
    // Maps userId -> Scheduled task for disconnection
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> disconnectTasks = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public void registerSession(String sessionId, Long userId, Long battleId) {
        sessionToUserMap.put(sessionId, userId);
        sessionToBattleMap.put(sessionId, battleId);
        
        // If they had a pending disconnect, cancel it!
        ScheduledFuture<?> task = disconnectTasks.remove(userId);
        if (task != null) {
            task.cancel(false);
            // Notify frontend that player reconnected
            Map<String, Object> msg = new HashMap<>();
            msg.put("userId", userId);
            msg.put("status", "RECONNECTED");
            messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/status", (Object) msg);
        }
    }

    @EventListener
    public void onDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        Long userId = sessionToUserMap.remove(sessionId);
        Long battleId = sessionToBattleMap.remove(sessionId);
        
        if (userId != null && battleId != null) {
            Battle battle = battleRepository.findById(battleId).orElse(null);
            if (battle == null || !"ACTIVE".equals(battle.getStatus())) return;
            
            // Check if user is a participant
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !participantRepository.existsByBattleAndUser(battle, user)) return;

            // Notify frontend that player disconnected
            Map<String, Object> msg = new HashMap<>();
            msg.put("userId", userId);
            msg.put("status", "DISCONNECTED_GRACE_PERIOD");
            msg.put("gracePeriodSeconds", 3);
            messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/status", (Object) msg);

            // Schedule immediate forfeit (3 seconds buffer for brief connection blips)
            ScheduledFuture<?> task = scheduler.schedule(() -> {
                executeDisconnectTKO(battleId, userId);
            }, 3, TimeUnit.SECONDS);
            
            disconnectTasks.put(userId, task);
        }
    }
    
    private void executeDisconnectTKO(Long battleId, Long disconnectedUserId) {
        disconnectTasks.remove(disconnectedUserId);
        
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (battle == null || !"ACTIVE".equals(battle.getStatus())) return;
        
        User disconnectedUser = userRepository.findById(disconnectedUserId).orElse(null);
        if (disconnectedUser == null) return;
        
        battleTimerService.completeBattleWithForfeit(battle, disconnectedUser);
    }
}
