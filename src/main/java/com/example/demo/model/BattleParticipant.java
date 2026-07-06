package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle_participants")
public class BattleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "battle_id")
    private Battle battle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime joinedAt;

    private Boolean checkedIn = false;
    private String participantNumber;
    private String seatNumber;
    private String qrPassCode;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
        if (qrPassCode == null) qrPassCode = java.util.UUID.randomUUID().toString();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Battle getBattle() { return battle; }
    public void setBattle(Battle battle) { this.battle = battle; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public Boolean getCheckedIn() { return checkedIn != null ? checkedIn : false; }
    public void setCheckedIn(Boolean checkedIn) { this.checkedIn = checkedIn; }

    public String getParticipantNumber() { return participantNumber; }
    public void setParticipantNumber(String participantNumber) { this.participantNumber = participantNumber; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getQrPassCode() { return qrPassCode; }
    public void setQrPassCode(String qrPassCode) { this.qrPassCode = qrPassCode; }
}
