package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class WebSocketEventListener {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("user")) {
            User user = (User) sessionAttributes.get("user");
            if (user != null && user.getId() != null) {
                updateUserStatusAsync(user.getId(), true);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("user")) {
            User user = (User) sessionAttributes.get("user");
            if (user != null && user.getId() != null) {
                updateUserStatusAsync(user.getId(), false);
            }
        }
    }

    private void updateUserStatusAsync(Long userId, boolean isOnline) {
        if (userId == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                // 1. Quick isolated DB update with immediate connection release
                userRepository.findById(userId).ifPresent(user -> {
                    user.setLastActiveAt(now);
                    userRepository.save(user);
                });

                // 2. Broadcast status on websocket without holding DB connection
                Map<String, Object> statusMessage = new HashMap<>();
                statusMessage.put("userId", userId);
                statusMessage.put("isOnline", isOnline);
                statusMessage.put("lastActiveAt", now);

                messagingTemplate.convertAndSend((String) "/topic/online-status", (Object) statusMessage);
            } catch (Exception ignored) {
                // Ignore transient errors on disconnect
            }
        });
    }
}
