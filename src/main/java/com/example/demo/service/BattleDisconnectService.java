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

            // Notify frontend that player disconnected (start grace period UI)
            Map<String, Object> msg = new HashMap<>();
            msg.put("userId", userId);
            msg.put("status", "DISCONNECTED_GRACE_PERIOD");
            msg.put("gracePeriodSeconds", 45);
            messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/status", (Object) msg);

            // Schedule TKO
            ScheduledFuture<?> task = scheduler.schedule(() -> {
                executeDisconnectTKO(battleId, userId);
            }, 45, TimeUnit.SECONDS);
            
            disconnectTasks.put(userId, task);
        }
    }
    
    private void executeDisconnectTKO(Long battleId, Long disconnectedUserId) {
        disconnectTasks.remove(disconnectedUserId);
        
        Battle battle = battleRepository.findById(battleId).orElse(null);
        if (battle == null || !"ACTIVE".equals(battle.getStatus())) return;
        
        List<BattleParticipant> participants = participantRepository.findByBattle(battle);
        if (participants.size() != 2) return;
        
        User disconnectedUser = participants.get(0).getUser().getId().equals(disconnectedUserId) ? participants.get(0).getUser() : participants.get(1).getUser();
        User winnerUser = participants.get(0).getUser().getId().equals(disconnectedUserId) ? participants.get(1).getUser() : participants.get(0).getUser();
        
        battle.setIsLive(false);
        battle.setStatus("COMPLETED");
        battle.setWinner(winnerUser);
        battle.setWinner2(disconnectedUser);
        
        // Update ELO rating (winnerUser wins)
        battleTimerService.updateElo(winnerUser, disconnectedUser, false, true);
        
        userRepository.save(winnerUser);
        userRepository.save(disconnectedUser);
        battleRepository.save(battle);
        
        // Gamification logic could go here (similar to TimerService)
        // For now, just mark TKO and notify

        Map<String, Object> msg = new HashMap<>();
        msg.put("status", "COMPLETED");
        msg.put("reason", "DISCONNECT_TKO");
        msg.put("winnerId", winnerUser.getId());
        msg.put("loserId", disconnectedUser.getId());
        messagingTemplate.convertAndSend("/topic/battle/" + battleId + "/status", (Object) msg);
    }
}
