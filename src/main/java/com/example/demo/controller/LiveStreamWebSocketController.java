package com.example.demo.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@Controller
public class LiveStreamWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Handle chat messages in live stream
    @MessageMapping("/live/{roomId}/chat")
    public void handleLiveChat(@DestinationVariable String roomId, @Payload Map<String, Object> message) {
        messagingTemplate.convertAndSend("/topic/live/" + roomId + "/chat", (Object) message);
    }

    // Handle WebRTC Signaling (offer, answer, candidate)
    @MessageMapping("/live/{roomId}/signal")
    public void handleSignaling(@DestinationVariable String roomId, @Payload Map<String, Object> signalData) {
        String targetUser = (String) signalData.get("target");
        if (targetUser != null && !targetUser.isEmpty()) {
            messagingTemplate.convertAndSend("/queue/live/" + roomId + "/" + targetUser, (Object) signalData);
        } else {
            messagingTemplate.convertAndSend("/topic/live/" + roomId + "/signal", (Object) signalData);
        }
    }

    // Handle Join Requests
    @MessageMapping("/live/{roomId}/request-join")
    public void handleJoinRequest(@DestinationVariable String roomId, @Payload Map<String, Object> request) {
        messagingTemplate.convertAndSend("/topic/live/" + roomId + "/join-requests", (Object) request);
    }
}
