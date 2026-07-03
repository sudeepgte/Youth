package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle_submissions")
public class BattleSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "battle_id")
    private Battle battle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 1000)
    private String submissionUrl; // GitHub link, YouTube link, image URL, etc.

    @Column(length = 2000)
    private String description;

    // Optional secondary URL (e.g., demo URL for coding, time-lapse for art)
    @Column(length = 1000)
    private String secondaryUrl;

    private Integer voteCount = 0;

    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Battle getBattle() { return battle; }
    public void setBattle(Battle battle) { this.battle = battle; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSubmissionUrl() { return submissionUrl; }
    public void setSubmissionUrl(String submissionUrl) { this.submissionUrl = submissionUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSecondaryUrl() { return secondaryUrl; }
    public void setSecondaryUrl(String secondaryUrl) { this.secondaryUrl = secondaryUrl; }

    public Integer getVoteCount() { return voteCount != null ? voteCount : 0; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
