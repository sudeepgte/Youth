package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle_live_comments")
public class BattleLiveComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "battle_id")
    private Battle battle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 500)
    private String message;

    private boolean isDeleted = false;
    private boolean isReported = false;

    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Battle getBattle() { return battle; }
    public void setBattle(Battle battle) { this.battle = battle; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }

    public boolean isReported() { return isReported; }
    public void setReported(boolean isReported) { this.isReported = isReported; }
}
