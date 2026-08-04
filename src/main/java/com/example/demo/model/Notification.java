package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The recipient of the notification

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor; // The user who triggered the notification

    private String message;
    private String type; // e.g., "FOLLOW", "LIKE", "COMMENT", "COLLAB"
    private boolean isRead = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private Long postId;

    public Notification() {
    }

    public Notification(User user, User actor, String message, String type) {
        this.user = user;
        this.actor = actor;
        this.message = message;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public Notification(User user, User actor, String message, String type, Long postId) {
        this.user = user;
        this.actor = actor;
        this.message = message;
        this.type = type;
        this.postId = postId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
