package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tournament_participants")
public class TournamentParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer seed;
    
    // ACTIVE, ELIMINATED
    private String status = "ACTIVE";
    
    private Boolean checkedIn = false;
    private String participantNumber;
    private String qrPassCode;

    @PrePersist
    public void prePersist() {
        if (qrPassCode == null) qrPassCode = java.util.UUID.randomUUID().toString();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getCheckedIn() { return checkedIn != null ? checkedIn : false; }
    public void setCheckedIn(Boolean checkedIn) { this.checkedIn = checkedIn; }

    public String getParticipantNumber() { return participantNumber; }
    public void setParticipantNumber(String participantNumber) { this.participantNumber = participantNumber; }

    public String getQrPassCode() { return qrPassCode; }
    public void setQrPassCode(String qrPassCode) { this.qrPassCode = qrPassCode; }
}
