package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"battle_id", "voter_id"})
})
public class BattleVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "battle_id")
    private Battle battle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "voter_id")
    private User voter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "submission_id")
    private BattleSubmission submission;

    private LocalDateTime votedAt;

    @PrePersist
    public void prePersist() {
        if (votedAt == null) votedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Battle getBattle() { return battle; }
    public void setBattle(Battle battle) { this.battle = battle; }

    public User getVoter() { return voter; }
    public void setVoter(User voter) { this.voter = voter; }

    public BattleSubmission getSubmission() { return submission; }
    public void setSubmission(BattleSubmission submission) { this.submission = submission; }

    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }
}
