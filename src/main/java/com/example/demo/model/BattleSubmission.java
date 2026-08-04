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

    private Integer judgeCreativity = 0;
    private Integer judgeQuality = 0;
    private Integer judgeSpeed = 0;
    private Integer judgePresentation = 0;
    private Integer judgeAccuracy = 0;
    private Integer judgeTotalScore = 0;

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

    public Integer getJudgeCreativity() { return judgeCreativity != null ? judgeCreativity : 0; }
    public void setJudgeCreativity(Integer judgeCreativity) { this.judgeCreativity = judgeCreativity; }

    public Integer getJudgeQuality() { return judgeQuality != null ? judgeQuality : 0; }
    public void setJudgeQuality(Integer judgeQuality) { this.judgeQuality = judgeQuality; }

    public Integer getJudgeSpeed() { return judgeSpeed != null ? judgeSpeed : 0; }
    public void setJudgeSpeed(Integer judgeSpeed) { this.judgeSpeed = judgeSpeed; }

    public Integer getJudgePresentation() { return judgePresentation != null ? judgePresentation : 0; }
    public void setJudgePresentation(Integer judgePresentation) { this.judgePresentation = judgePresentation; }

    public Integer getJudgeAccuracy() { return judgeAccuracy != null ? judgeAccuracy : 0; }
    public void setJudgeAccuracy(Integer judgeAccuracy) { this.judgeAccuracy = judgeAccuracy; }

    public Integer getJudgeTotalScore() { return judgeTotalScore != null ? judgeTotalScore : 0; }
    public void setJudgeTotalScore(Integer judgeTotalScore) { this.judgeTotalScore = judgeTotalScore; }
}
