package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.ChatService;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private User getUserFromSession(HttpSession session) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            Object authUser = request.getAttribute("authenticatedUser");
            if (authUser instanceof User) {
                return (User) authUser;
            }
        }

        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            return (User) sessionUser;
        }
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = null;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }

                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception e) {
                System.err.println("[DEBUG] Failed to recover user from session userId: " + userIdObj);
            }
        }
        return null;
    }

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessageSendingOperations messagingTemplate;

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations(HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            // Not logged in — return 401 silently (no error log needed)
            return ResponseEntity.status(401).build();
        }
        try {
            List<Conversation> convs = chatService.getUserConversations(user);
            return ResponseEntity.ok(convs);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable Long conversationId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<com.example.demo.model.MessageReadReceipt> seen = chatService.markMessagesAsSeen(conversationId, user);
            notifySenderOfSeen(seen);
            return ResponseEntity.ok(chatService.getChatHistory(conversationId, user));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/mark-seen/{conversationId}")
    public ResponseEntity<Void> markSeen(@PathVariable Long conversationId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            List<com.example.demo.model.MessageReadReceipt> seen = chatService.markMessagesAsSeen(conversationId, user);
            notifySenderOfSeen(seen);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/media/{conversationId}")
    public ResponseEntity<List<ChatMessage>> getMedia(@PathVariable Long conversationId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(chatService.getConversationMedia(conversationId, user));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/cleanup-vanish/{conversationId}")
    public ResponseEntity<Void> cleanupVanish(@PathVariable Long conversationId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            chatService.cleanupVanishMessages(conversationId, user);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/update-theme/{conversationId}")
    public ResponseEntity<Conversation> updateTheme(@PathVariable Long conversationId, @RequestParam String theme,
            HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(chatService.updateTheme(conversationId, theme, user));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/create-group")
    public ResponseEntity<Conversation> createGroup(@RequestBody Map<String, Object> payload, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        String name = (String) payload.get("name");
        List<?> participantIdsRaw = (List<?>) payload.get("participantIds");
        List<Long> participantIds = participantIdsRaw.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(chatService.createGroup(name, participantIds, user));
    }

    @GetMapping("/group/{conversationId}/participants")
    public ResponseEntity<?> getGroupParticipants(@PathVariable Long conversationId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return ResponseEntity.status(401).build();

        return conversationRepository.findById(conversationId)
                .map(conv -> {
                    boolean isParticipant = conv.getParticipants() != null
                            && conv.getParticipants().stream().anyMatch(p -> p.getId().equals(user.getId()));
                    if (!isParticipant) return ResponseEntity.status(403).build();
                    return ResponseEntity.ok(Map.of(
                            "conversationId", conv.getId(),
                            "type", conv.getType(),
                            "name", conv.getName(),
                            "creatorId", conv.getCreator() != null ? conv.getCreator().getId() : null,
                            "participants", conv.getParticipants()
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @PostMapping("/group/{conversationId}/add-participants")
    public ResponseEntity<Conversation> addGroupParticipants(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Object> payload,
            HttpSession session
    ) {
        User user = getUserFromSession(session);
        if (user == null) return ResponseEntity.status(401).build();

        List<?> idsRaw = (List<?>) payload.get("participantIds");
        List<Long> ids = idsRaw == null ? List.of() : idsRaw.stream()
                .map(x -> Long.valueOf(x.toString()))
                .collect(Collectors.toList());

        try {
            Conversation updated = chatService.addGroupParticipants(conversationId, ids, user);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/group/{conversationId}/remove-participant/{userId}")
    public ResponseEntity<Conversation> removeGroupParticipant(
            @PathVariable Long conversationId,
            @PathVariable Long userId,
            HttpSession session
    ) {
        User user = getUserFromSession(session);
        if (user == null) return ResponseEntity.status(401).build();

        try {
            Conversation updated = chatService.removeGroupParticipant(conversationId, userId, user);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/toggle-pin/{messageId}")
    public ResponseEntity<ChatMessage> togglePin(@PathVariable Long messageId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(chatService.togglePin(messageId, user));
    }

    @PostMapping("/share-post")
    public ResponseEntity<ChatMessage> sharePost(@RequestBody Map<String, Object> payload, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();
        // Always refresh and ensure it's in session
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        Long postId = Long.valueOf(payload.get("postId").toString());
        Long conversationId = Long.valueOf(payload.get("conversationId").toString());

        ChatMessage shared = chatService.sharePost(postId, conversationId, user);

        // Notify participants via WebSocket
        messagingTemplate.convertAndSendToUser(user.getId().toString(), "/queue/messages", shared);

        return ResponseEntity.ok(shared);
    }

    @PostMapping("/share-post-to-user")
    public ResponseEntity<ChatMessage> sharePostToUser(@RequestBody Map<String, Object> payload, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();

        Long postId = Long.valueOf(payload.get("postId").toString());
        Long recipientId = Long.valueOf(payload.get("recipientId").toString());

        ChatMessage shared = chatService.sharePostToUser(postId, recipientId, user);

        // Notify recipient via WebSocket
        messagingTemplate.convertAndSendToUser(recipientId.toString(), "/queue/messages", shared);
        messagingTemplate.convertAndSendToUser(user.getId().toString(), "/queue/messages", shared);

        return ResponseEntity.ok(shared);
    }

    private void notifySenderOfSeen(List<com.example.demo.model.MessageReadReceipt> seenReceipts) {
        if (seenReceipts == null || seenReceipts.isEmpty())
            return;
        // Notify the sender of each message that was newly seen
        for (com.example.demo.model.MessageReadReceipt receipt : seenReceipts) {
            messagingTemplate.convertAndSendToUser(
                    receipt.getMessage().getSender().getId().toString(),
                    "/queue/seen",
                    Map.of(
                            "conversationId", receipt.getMessage().getConversation().getId(),
                            "messageId", receipt.getMessage().getId(),
                            "seenBy", receipt.getUser().getId(),
                            "seenAt", receipt.getSeenAt()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(HttpSession session) {
        User currentUser = getUserFromSession(session);
        List<User> users = userRepository.findAll();
        if (currentUser != null) {
            users.removeIf(u -> u.getId().equals(currentUser.getId()));
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping("/followers")
    public ResponseEntity<List<User>> getFollowers(HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null)
            return ResponseEntity.status(401).build();
        // Refresh to get collections if needed
        user = userRepository.findById(user.getId()).orElse(user);
        return ResponseEntity.ok(List.copyOf(user.getFollowers()));
    }

    @GetMapping("/search-users")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query, HttpSession session) {
        User currentUser = getUserFromSession(session);
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(query);
        if (currentUser != null) {
            users.removeIf(u -> u.getId().equals(currentUser.getId()));
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }
        return ResponseEntity.ok(Map.of("count", chatService.getUnreadCount(user)));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadMedia(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String uploadDir = "src/main/resources/static/uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Also save to target to be immediately available
            String targetUploadDir = "target/classes/static/uploads/";
            Path targetUploadPath = Paths.get(targetUploadDir);
            if (!Files.exists(targetUploadPath)) {
                Files.createDirectories(targetUploadPath);
            }

            byte[] bytes = file.getBytes();
            Files.write(uploadPath.resolve(fileName), bytes);
            Files.write(targetUploadPath.resolve(fileName), bytes);
            System.out.println("[DEBUG] Successfully uploaded file: " + fileName);

            return ResponseEntity.ok(Map.of("url", "/uploads/" + fileName));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ERROR] Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/accept/{id}")
    public ResponseEntity<Conversation> acceptConversation(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Conversation accepted = chatService.acceptConversation(id, user);
        System.out.println("[DEBUG] acceptConversation succeeded. Status is now: " + accepted.getStatus());

        // Notify both participants
        for (User p : accepted.getParticipants()) {
            messagingTemplate.convertAndSendToUser(
                    p.getId().toString(),
                    "/queue/conversation-update",
                    accepted);
            System.out.println("[DEBUG] Notified user " + p.getId() + " about accepted conversation.");
        }

        return ResponseEntity.ok(accepted);
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<Void> rejectConversation(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        // Always refresh and ensure it's in session
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        // Find conversation to notify participants before deletion
        // We'll use a simple findById since we have conversational context
        conversationRepository.findById(id).ifPresent(conv -> {
            for (User p : conv.getParticipants()) {
                messagingTemplate.convertAndSendToUser(
                        p.getId().toString(),
                        "/queue/conversation-delete",
                        Map.of("conversationId", id));
                System.out.println("[DEBUG] Notified user " + p.getId() + " about rejected conversation.");
            }
        });

        chatService.rejectConversation(id, user);
        System.out.println("[DEBUG] rejectConversation succeeded.");
        return ResponseEntity.ok().build();
    }
}
