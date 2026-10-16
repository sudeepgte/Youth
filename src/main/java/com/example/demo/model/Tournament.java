package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    @Column(length = 2000)
    private String description;
    
    private String type; // e.g., Elimination
    
    private Integer maxParticipants;
    
    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDateTime tournamentStart;
    
    private Integer battleDurationMinutes;
    
    @Column(length = 1000)
    private String entryRules;
    
    // DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED
    private String status = "DRAFT";
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner_id")
    private User winner;

    @JsonIgnore
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentParticipant> participants = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentRound> rounds = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentBattle> tournamentBattles = new ArrayList<>();

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public LocalDateTime getRegistrationStart() { return registrationStart; }
    public void setRegistrationStart(LocalDateTime registrationStart) { this.registrationStart = registrationStart; }

    public LocalDateTime getRegistrationEnd() { return registrationEnd; }
    public void setRegistrationEnd(LocalDateTime registrationEnd) { this.registrationEnd = registrationEnd; }

    public LocalDateTime getTournamentStart() { return tournamentStart; }
    public void setTournamentStart(LocalDateTime tournamentStart) { this.tournamentStart = tournamentStart; }

    public Integer getBattleDurationMinutes() { return battleDurationMinutes; }
    public void setBattleDurationMinutes(Integer battleDurationMinutes) { this.battleDurationMinutes = battleDurationMinutes; }

    public String getEntryRules() { return entryRules; }
    public void setEntryRules(String entryRules) { this.entryRules = entryRules; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }

    public List<TournamentParticipant> getParticipants() { return participants; }
    public void setParticipants(List<TournamentParticipant> participants) { this.participants = participants; }

    public List<TournamentRound> getRounds() { return rounds; }
    public void setRounds(List<TournamentRound> rounds) { this.rounds = rounds; }

    public List<TournamentBattle> getTournamentBattles() { return tournamentBattles; }
    public void setTournamentBattles(List<TournamentBattle> tournamentBattles) { this.tournamentBattles = tournamentBattles; }
}
