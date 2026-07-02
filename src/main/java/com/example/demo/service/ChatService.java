package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.model.MessageStatus;
import com.example.demo.model.MessageType;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MessageReadReceiptRepository readReceiptRepository;

    public ChatMessage getChatMessage(Long id) {
        return chatMessageRepository.findById(id).orElse(null);
    }

    public List<Conversation> getUserConversations(User user) {
        List<Conversation> convs = conversationRepository.findAllByUserOrderByLastMessageTimeDesc(user);
        for (Conversation c : convs) {
            c.setUnread(chatMessageRepository.existsUnreadForUser(c, user));
        }
        return convs;
    }

    private Conversation getConversationOrThrow(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }

    private void requireParticipant(Conversation conv, User user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("Unauthorized");
        }
        boolean isParticipant = conv.getParticipants() != null
                && conv.getParticipants().stream().anyMatch(p -> Objects.equals(p.getId(), user.getId()));
        if (!isParticipant) {
            throw new RuntimeException("Unauthorized");
        }
    }

    private void requireGroupAdmin(Conversation conv, User user) {
        requireParticipant(conv, user);
        if (conv.getType() != Conversation.ConversationType.GROUP) {
            throw new RuntimeException("Not a group conversation");
        }
        if (conv.getCreator() == null || !Objects.equals(conv.getCreator().getId(), user.getId())) {
            throw new RuntimeException("Only group admin can manage members");
        }
    }

    @Transactional
    public ChatMessage sendMessage(User sender, Long destinationId, String content, String mediaUrl, Long parentId,
            boolean isGroup, boolean isForwarded) {
        Conversation conv;
        if (isGroup) {
            conv = conversationRepository.findById(destinationId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            requireParticipant(conv, sender);
        } else {
            User recipient = userRepository.findById(destinationId)
                    .orElseThrow(() -> new RuntimeException("Recipient not found"));
            conv = conversationRepository.findDirectConversationBetweenUsers(sender, recipient)
                    .orElseGet(() -> {
                        Conversation newConv = new Conversation();
                        newConv.setType(Conversation.ConversationType.DIRECT);
                        List<User> participants = new ArrayList<>();
                        participants.add(sender);
                        participants.add(recipient);
                        newConv.setParticipants(participants);
                        newConv.setCreator(sender);
                        newConv.setStatus(Conversation.ConversationStatus.PENDING);
                        newConv.setLastMessage("Conversation started");
                        newConv.setLastMessageTime(LocalDateTime.now());
                        return conversationRepository.save(newConv);
                    });
        }

        ChatMessage message = new ChatMessage(conv, sender, content, mediaUrl);
        message.setVanish(conv.isVanishModeEnabled());
        message.setForwarded(isForwarded);

        // Determine MessageType
        if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
            message.setMessageType(MessageType.MEDIA);
            System.out.println("[DEBUG] ChatService.sendMessage: Saving MEDIA message with URL: " + mediaUrl);
        } else {
            message.setMessageType(MessageType.TEXT);
            System.out.println("[DEBUG] ChatService.sendMessage: Saving TEXT message");
        }

        if (parentId != null) {
            ChatMessage parent = chatMessageRepository.findById(parentId).orElse(null);
            message.setParentMessage(parent);
        }

        System.out.println("[DEBUG] ChatService.sendMessage: Saving message: content=" + content + ", mediaUrl="
                + mediaUrl + ", type=" + message.getMessageType());
        ChatMessage saved = chatMessageRepository.save(message);
        System.out.println("[DEBUG] ChatService.sendMessage: Message saved with ID: " + saved.getId());

        conv.setLastMessage(content != null && !content.trim().isEmpty() ? content : "Media shared");
        conv.setLastMessageTime(LocalDateTime.now());
        conversationRepository.save(conv);

        return saved;
    }

    @Transactional
    public Conversation createGroup(String name, List<Long> userIds, User creator) {
        Conversation group = new Conversation();
        group.setName(name);
        group.setType(Conversation.ConversationType.GROUP);
        group.setCreator(creator);

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        if (creator != null && creator.getId() != null) {
            uniqueIds.add(creator.getId());
        }
        if (userIds != null) {
            uniqueIds.addAll(userIds);
        }

        List<User> participants = new ArrayList<>();
        for (Long id : uniqueIds) {
            if (id == null) continue;
            userRepository.findById(id).ifPresent(participants::add);
        }

        // Safety: always keep creator inside participants
        if (creator != null && creator.getId() != null
                && participants.stream().noneMatch(u -> Objects.equals(u.getId(), creator.getId()))) {
            userRepository.findById(creator.getId()).ifPresent(participants::add);
        }

        group.setParticipants(participants);
        group.setLastMessage("Group created");
        group.setLastMessageTime(LocalDateTime.now());

        return conversationRepository.save(group);
    }

    @Transactional
    public Conversation addGroupParticipants(Long conversationId, List<Long> userIdsToAdd, User actor) {
        Conversation conv = getConversationOrThrow(conversationId);
        requireGroupAdmin(conv, actor);

        if (userIdsToAdd == null || userIdsToAdd.isEmpty()) {
            return conv;
        }

        LinkedHashSet<Long> existingIds = new LinkedHashSet<>();
        if (conv.getParticipants() != null) {
            conv.getParticipants().forEach(u -> existingIds.add(u.getId()));
        }

        for (Long id : userIdsToAdd) {
            if (id == null) continue;
            if (existingIds.contains(id)) continue;
            userRepository.findById(id).ifPresent(u -> {
                conv.getParticipants().add(u);
                existingIds.add(id);
            });
        }

        return conversationRepository.save(conv);
    }

    @Transactional
    public Conversation removeGroupParticipant(Long conversationId, Long userIdToRemove, User actor) {
        Conversation conv = getConversationOrThrow(conversationId);
        requireGroupAdmin(conv, actor);

        if (userIdToRemove == null) {
            return conv;
        }
        // Don't allow removing the admin/creator
        if (conv.getCreator() != null && Objects.equals(conv.getCreator().getId(), userIdToRemove)) {
            throw new RuntimeException("Group admin cannot be removed");
        }
        if (conv.getParticipants() != null) {
            conv.getParticipants().removeIf(u -> Objects.equals(u.getId(), userIdToRemove));
        }
        return conversationRepository.save(conv);
    }

    @Transactional
    public Conversation toggleVanishMode(Long conversationId, boolean enabled) {
        Conversation conv = getConversationOrThrow(conversationId);
        conv.setVanishModeEnabled(enabled);
        Conversation saved = conversationRepository.save(conv);
        
        if (!enabled) {
            // Aggressive cleanup when toggling OFF
            wipeVanishMessages(conversationId);
        }
        
        return saved;
    }

    @Transactional
    public Conversation toggleVanishMode(Long conversationId, boolean enabled, User actor) {
        Conversation conv = getConversationOrThrow(conversationId);
        requireParticipant(conv, actor);
        conv.setVanishModeEnabled(enabled);
        Conversation saved = conversationRepository.save(conv);

        if (!enabled) {
            wipeVanishMessages(conversationId, actor);
        }

        return saved;
    }

    @Transactional
    public Conversation updateTheme(Long conversationId, String theme) {
        Conversation conv = getConversationOrThrow(conversationId);
        conv.setTheme(theme);
        return conversationRepository.save(conv);
    }

    @Transactional
    public Conversation updateTheme(Long conversationId, String theme, User actor) {
        Conversation conv = getConversationOrThrow(conversationId);
        requireParticipant(conv, actor);
        conv.setTheme(theme);
        return conversationRepository.save(conv);
    }

    @Transactional
    public void cleanupVanishMessages(Long conversationId) {
        Conversation conv = getConversationOrThrow(conversationId);
        System.out.println("[DEBUG] Cleaning up SEEN vanish messages for conversation: " + conversationId);
        // Delete receipts and reactions first to satisfy foreign key constraints
        readReceiptRepository.deleteByConversationAndIsVanishTrueAndStatus(conv, MessageStatus.SEEN);
        chatMessageRepository.deleteReactionsByConversationAndIsVanishTrueAndStatus(conversationId, MessageStatus.SEEN.name());
        chatMessageRepository.deleteByConversationAndIsVanishTrueAndStatus(conv, MessageStatus.SEEN);
    }

    @Transactional
    public void cleanupVanishMessages(Long conversationId, User actor) {
        Conversation conv = getConversationOrThrow(conversationId);
        requireParticipant(conv, actor);
        cleanupVanishMessages(conversationId);
    }

    @Transactional
    public void wipeVanishMessages(Long conversationId) {
        Conversation conv = getConversationOrThrow(conversationId);
        System.out.println("[DEBUG] Wiping ALL vanish messages for conversation: " + conversationId);
        // Delete receipts and reactions first
        readReceiptRepository.deleteByConversationAndIsVanishTrue(conv);
        chatMessageRepository.deleteReactionsByConversationAndIsVanishTrue(conversationId);
        chatMessageRepository.deleteByConversationAndIsVanishTrue(conv);
    }

    @Transactional
    public void wipeVanishMessages(Long conversationId, User actor) {
        Conversation conv = getConversationOrThrow(conversationId);
        requireParticipant(conv, actor);
        wipeVanishMessages(conversationId);
    }

    public List<ChatMessage> getChatHistory(Long conversationId, User user) {
        Conversation conversation = getConversationOrThrow(conversationId);
        requireParticipant(conversation, user);
        return chatMessageRepository.findByConversationOrderByTimestampAsc(conversation);
    }

    public List<ChatMessage> getConversationMedia(Long conversationId, User user) {
        Conversation conversation = getConversationOrThrow(conversationId);
        requireParticipant(conversation, user);
        return chatMessageRepository.findByConversationAndMediaUrlIsNotNullOrderByTimestampDesc(conversation);
    }

    @Transactional
    public List<MessageReadReceipt> markMessagesAsSeen(Long conversationId, User user) {
        Conversation conversation = getConversationOrThrow(conversationId);
        requireParticipant(conversation, user);

        List<ChatMessage> history = chatMessageRepository.findByConversationOrderByTimestampAsc(conversation);
        List<MessageReadReceipt> newReceipts = new ArrayList<>();

        for (ChatMessage msg : history) {
            if (!msg.getSender().getId().equals(user.getId())
                    && !readReceiptRepository.existsByMessageAndUser(msg, user)) {
                msg.setStatus(MessageStatus.SEEN);
                msg.setSeenAt(LocalDateTime.now());
                chatMessageRepository.save(msg);
                newReceipts.add(new MessageReadReceipt(msg, user));
            }
        }
        return readReceiptRepository.saveAll(newReceipts);
    }

    public long getUnreadCount(User user) {
        List<Conversation> convs = conversationRepository.findAllByUserOrderByLastMessageTimeDesc(user);
        return chatMessageRepository.countUnreadForUserInConversations(convs, user);
    }

    @Transactional
    public void deleteMessage(Long messageId, User sender) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        requireParticipant(message.getConversation(), sender);
        if (!message.getSender().getId().equals(sender.getId())) {
            throw new RuntimeException("Unauthorized to delete this message");
        }
        
        message.setContent("Message unsent");
        message.setMediaUrl(null);
        chatMessageRepository.save(message);
    }

    @Transactional
    public ChatMessage reactToMessage(Long messageId, String reaction, User reactor) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        requireParticipant(message.getConversation(), reactor);

        Map<User, String> reactions = message.getReactions();
        if (reactions.containsKey(reactor) && reactions.get(reactor).equals(reaction)) {
            reactions.remove(reactor);
        } else {
            reactions.put(reactor, reaction);
        }

        return chatMessageRepository.save(message);
    }

    @Transactional
    public ChatMessage togglePin(Long messageId, User user) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        requireParticipant(message.getConversation(), user);

        message.setPinned(!message.isPinned());
        message.setPinnedAt(message.isPinned() ? LocalDateTime.now() : null);

        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getPinnedMessages(Long conversationId, User user) {
        Conversation conversation = getConversationOrThrow(conversationId);
        requireParticipant(conversation, user);
        return chatMessageRepository.findByConversationAndIsPinnedTrueOrderByTimestampDesc(conversation);
    }

    @Transactional
    public ChatMessage sharePost(Long postId, Long conversationId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        Conversation conv = getConversationOrThrow(conversationId);
        requireParticipant(conv, user);

        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setSender(user);
        msg.setContent("Forwarded a post: " + post.getContent());
        msg.setMediaUrl(post.getMediaUrl());
        msg.setTimestamp(LocalDateTime.now());
        msg.setStatus(MessageStatus.SENT);

        return chatMessageRepository.save(msg);
    }

    @Transactional
    public ChatMessage sharePostToUser(Long postId, Long recipientId, User sender) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        Conversation conv = conversationRepository.findDirectConversationBetweenUsers(sender, recipient)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setType(Conversation.ConversationType.DIRECT);
                    List<User> participants = new ArrayList<>();
                    participants.add(sender);
                    participants.add(recipient);
                    newConv.setParticipants(participants);
                    newConv.setCreator(sender);
                    newConv.setStatus(Conversation.ConversationStatus.PENDING);
                    newConv.setLastMessage("Post shared");
                    newConv.setLastMessageTime(LocalDateTime.now());
                    return conversationRepository.save(newConv);
                });

        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setContent("Forwarded a post: " + (post.getContent() != null ? post.getContent() : "Post"));
        msg.setMediaUrl(post.getMediaUrl());
        msg.setTimestamp(LocalDateTime.now());
        msg.setStatus(MessageStatus.SENT);
        msg.setMessageType(MessageType.MEDIA);

        ChatMessage saved = chatMessageRepository.save(msg);

        conv.setLastMessage(msg.getContent());
        conv.setLastMessageTime(msg.getTimestamp());
        conversationRepository.save(conv);

        return saved;
    }

    @Transactional
    public Conversation acceptConversation(Long id, User user) {
        Conversation conv = getConversationOrThrow(id);
        requireParticipant(conv, user);

        System.out.println("[DEBUG] ChatService.acceptConversation: convId=" + id + ", creator="
                + (conv.getCreator() != null ? conv.getCreator().getId() : "NULL") + ", userParams=" + user.getId());

        // Only recipient can accept
        if (conv.getCreator() != null && conv.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Sender cannot accept their own request");
        }
        conv.setStatus(Conversation.ConversationStatus.ACCEPTED);
        return conversationRepository.save(conv);
    }

    @Transactional
    public void rejectConversation(Long id, User user) {
        Conversation conv = getConversationOrThrow(id);
        requireParticipant(conv, user);

        System.out.println("[DEBUG] ChatService.rejectConversation: convId=" + id + ", creator="
                + (conv.getCreator() != null ? conv.getCreator().getId() : "NULL") + ", userParams=" + user.getId());

        if (conv.getCreator() != null && conv.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Sender cannot reject their own request");
        }
        // Instead of deleting, we could mark as REJECTED, but usually deletion is
        // cleaner for chat requests
        conversationRepository.delete(conv);
    }
}
