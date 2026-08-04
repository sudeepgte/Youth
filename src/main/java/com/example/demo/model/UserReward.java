package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class UserReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secret_reward_id", nullable = false)
    private SecretRewardPartner secretReward;

    private LocalDateTime issueDate;
    private LocalDateTime expiryDate;

    // AVAILABLE, REDEEMED, EXPIRED
    private String status;

    @Column(unique = true, nullable = false)
    private String rewardCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public SecretRewardPartner getSecretReward() { return secretReward; }
    public void setSecretReward(SecretRewardPartner secretReward) { this.secretReward = secretReward; }
    public LocalDateTime getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDateTime issueDate) { this.issueDate = issueDate; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRewardCode() { return rewardCode; }
    public void setRewardCode(String rewardCode) { this.rewardCode = rewardCode; }
}
