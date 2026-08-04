package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle_gifts")
public class BattleGift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "battle_id")
    private Battle battle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    private String giftType; // ROSE, GIFT_BOX, DIAMOND, FIRE, CROWN

    private Integer coinsCost;

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

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public String getGiftType() { return giftType; }
    public void setGiftType(String giftType) { this.giftType = giftType; }

    public Integer getCoinsCost() { return coinsCost; }
    public void setCoinsCost(Integer coinsCost) { this.coinsCost = coinsCost; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
