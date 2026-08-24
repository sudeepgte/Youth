package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendNotification(Long userId, String title, String message, String icon, String link) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("message", message);
        payload.put("icon", icon != null ? icon : "fas fa-bell");
        payload.put("link", link != null ? link : "javascript:void(0)");
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", payload);
    }
}
