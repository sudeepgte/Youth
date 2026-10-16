package com.example.demo.controller;

import com.example.demo.service.MatchmakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Map;

@Controller
public class MatchmakingWebSocketController {

    @Autowired
    private MatchmakingService matchmakingService;

    @MessageMapping("/matchmaking/join")
    public void joinQueue(@Payload Map<String, String> payload, Principal principal) {
        if (principal == null) return;
        Long userId = Long.parseLong(principal.getName());
        
        String category = payload.get("category");
        String skill = payload.get("skill");
        int duration = Integer.parseInt(payload.getOrDefault("duration", "3"));
        
        matchmakingService.joinQueue(userId, category, skill, duration);
    }

    @MessageMapping("/matchmaking/leave")
    public void leaveQueue(Principal principal) {
        if (principal == null) return;
        Long userId = Long.parseLong(principal.getName());
        matchmakingService.leaveQueue(userId);
    }

    @MessageMapping("/lobby/ready")
    public void setLobbyReady(@Payload Map<String, Long> payload, Principal principal) {
        if (principal == null) return;
        Long userId = Long.parseLong(principal.getName());
        Long battleId = payload.get("battleId");
        
        if (battleId != null) {
            matchmakingService.setPlayerReady(battleId, userId);
        }
    }
}
