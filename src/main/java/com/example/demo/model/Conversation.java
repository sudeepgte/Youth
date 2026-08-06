package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Null for DIRECT chats, set for GROUP chats

    @Enumerated(EnumType.STRING)
    private ConversationType type = ConversationType.DIRECT;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "conversation_participants", joinColumns = @JoinColumn(name = "conversation_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> participants = new ArrayList<>();

    @ManyToOne
    private User creator;

    @Column(length = 500)
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    @Transient
    private boolean unread;

    private boolean vanishModeEnabled = false;

    private String theme = "default";

    public enum ConversationType {
        DIRECT, GROUP, ONE_TO_ONE
    }

    public enum ConversationStatus {
        PENDING, ACCEPTED, REJECTED
    }

    @Enumerated(EnumType.STRING)
    private ConversationStatus status = ConversationStatus.ACCEPTED;

    public Conversation() {
    }

    public Conversation(List<User> participants, ConversationType type) {
        this.participants = participants;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public boolean isVanishModeEnabled() {
        return vanishModeEnabled;
    }

    public void setVanishModeEnabled(boolean vanishModeEnabled) {
        this.vanishModeEnabled = vanishModeEnabled;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }
}
