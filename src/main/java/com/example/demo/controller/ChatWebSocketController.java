package com.example.demo.controller;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.Conversation;
import com.example.demo.model.User;
import com.example.demo.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private com.example.demo.repository.UserRepository userRepository;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Map<String, Object> payload) {
        Long senderId = (payload.get("senderId") != null) ? Long.valueOf(payload.get("senderId").toString()) : null;
        Long destinationId = (payload.get("recipientId") != null) ? Long.valueOf(payload.get("recipientId").toString()) : null;
        String content = (String) payload.get("content");
        String mediaUrl = (String) payload.get("mediaUrl");
        Long parentId = (payload.get("parentId") != null) ? Long.valueOf(payload.get("parentId").toString()) : null;
        boolean isGroup = Boolean.TRUE.equals(payload.get("isGroup"));
        boolean isForwarded = Boolean.TRUE.equals(payload.get("isForwarded"));

        if (senderId == null || destinationId == null)
            return;

        User sender = userRepository.findById(senderId).orElse(null);
        if (sender == null)
            return;

        ChatMessage message;
        try {
            message = chatService.sendMessage(
                    sender,
                    destinationId,
                    content,
                    mediaUrl,
                    parentId,
                    isGroup,
                    isForwarded);
        } catch (RuntimeException ex) {
            return;
        }
        Conversation conv = message.getConversation();

        // Broadcast to participants
        for (User p : conv.getParticipants()) {
            messagingTemplate.convertAndSendToUser(p.getId().toString(), "/queue/messages", message);
            messagingTemplate.convertAndSendToUser(p.getId().toString(), "/queue/conversation-update", conv);
        }
    }

    @MessageMapping("/chat.react")
    public void reactToMessage(@Payload Map<String, Object> payload) {
        if (payload.get("messageId") == null || payload.get("reaction") == null || payload.get("senderId") == null)
            return;
        Long messageId = Long.valueOf(payload.get("messageId").toString());
        String reaction = payload.get("reaction").toString();
        Long senderId = Long.valueOf(payload.get("senderId").toString());

        User reactor = new User();
        reactor.setId(senderId);

        ChatMessage message;
        try {
            message = chatService.reactToMessage(messageId, reaction, reactor);
        } catch (RuntimeException ex) {
            return;
        }

        for (User p : message.getConversation().getParticipants()) {
            messagingTemplate.convertAndSendToUser(p.getId().toString(), "/queue/reaction", message);
        }
    }

    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload Map<String, Object> payload) {
        if (payload.get("messageId") == null || payload.get("senderId") == null)
            return;
        Long messageId = Long.valueOf(payload.get("messageId").toString());
        Long senderId = Long.valueOf(payload.get("senderId").toString());

        User sender = userRepository.findById(senderId).orElse(null);
        if (sender == null)
            return;

        ChatMessage message = chatService.getChatMessage(messageId);
        if (message == null)
            return;

        Conversation conv = message.getConversation();
        try {
            chatService.deleteMessage(messageId, sender);
        } catch (RuntimeException ex) {
            return;
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("messageId", messageId);
        response.put("conversationId", conv.getId());

        for (User p : conv.getParticipants()) {
            messagingTemplate.convertAndSendToUser(p.getId().toString(), "/queue/delete", response);
        }
    }

    @MessageMapping("/chat.typing")
    public void sendTypingStatus(@Payload Map<String, Object> payload) {
        if (payload.get("senderId") == null)
            return;

        if (payload.get("conversationId") != null) {
            String topic = "/topic/chat." + payload.get("conversationId") + ".typing";
            messagingTemplate.convertAndSend(topic, (Object) payload);
        } else if (payload.get("recipientId") != null) {
            String recipientId = payload.get("recipientId").toString();
            messagingTemplate.convertAndSendToUser(recipientId, "/queue/typing", (Object) payload);
        }
    }

    @MessageMapping("/chat.vanish")
    public void sendVanishStatus(@Payload Map<String, Object> payload) {
        if (payload.get("conversationId") == null || payload.get("enabled") == null || payload.get("senderId") == null)
            return;
        Long conversationId = Long.valueOf(payload.get("conversationId").toString());
        boolean enabled = (boolean) payload.get("enabled");
        Long senderId = Long.valueOf(payload.get("senderId").toString());
        User actor = userRepository.findById(senderId).orElse(null);
        if (actor == null) return;

        Conversation conv;
        try {
            conv = chatService.toggleVanishMode(conversationId, enabled, actor);
        } catch (RuntimeException ex) {
            return;
        }

        for (User p : conv.getParticipants()) {
            messagingTemplate.convertAndSendToUser(p.getId().toString(), "/queue/vanish", payload);
        }
    }

    @MessageMapping("/chat.theme")
    public void sendThemeStatus(@Payload Map<String, Object> payload) {
        if (payload.get("conversationId") == null || payload.get("theme") == null || payload.get("senderId") == null)
            return;
        Long conversationId = Long.valueOf(payload.get("conversationId").toString());
        String theme = payload.get("theme").toString();
        Long senderId = Long.valueOf(payload.get("senderId").toString());
        User actor = userRepository.findById(senderId).orElse(null);
        if (actor == null) return;

        Conversation conv;
        try {
            conv = chatService.updateTheme(conversationId, theme, actor);
        } catch (RuntimeException ex) {
            return;
        }

        for (User p : conv.getParticipants()) {
            messagingTemplate.convertAndSendToUser(p.getId().toString(), "/queue/theme", payload);
        }
    }

    // WebRTC Signaling for 1-on-1 Voice/Video Calls
    @MessageMapping("/chat.call")
    public void handleCallSignaling(@Payload Map<String, Object> payload) {
        if (payload.get("recipientId") == null) return;
        String recipientId = payload.get("recipientId").toString();
        
        // Relay the signaling message directly to the target recipient
        messagingTemplate.convertAndSendToUser(recipientId, "/queue/call", (Object) payload);
    }
}
