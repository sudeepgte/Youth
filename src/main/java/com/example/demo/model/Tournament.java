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
    private Double entryFee = 0.0;
    private Double adminCommissionPercent = 0.0;
    @Column(length = 1000)
    private String customPrizes;
    
    private Double prize1Percent = 60.0;
    private Double prize2Percent = 30.0;
    private Double prize3Percent = 10.0;
    
    @Column(length = 1000)
    private String entryRules;
    
    // DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED
    private String status = "DRAFT";
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner_id")
    private User winner;

    private String mode = "ONLINE";
    private String category;
    private String teamType = "Solo";
    private String venue;
    private String eventDate;
    private String eventTime;
    @Column(length = 1000)
    private String bannerUrl;
    private Boolean judgeScoringEnabled = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tournament_judges",
        joinColumns = @JoinColumn(name = "tournament_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> judges = new ArrayList<>();

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

    public Double getEntryFee() { return entryFee; }
    public void setEntryFee(Double entryFee) { this.entryFee = entryFee; }
    public Double getAdminCommissionPercent() { return adminCommissionPercent; }
    public void setAdminCommissionPercent(Double adminCommissionPercent) { this.adminCommissionPercent = adminCommissionPercent; }
    public String getCustomPrizes() { return customPrizes; }
    public void setCustomPrizes(String customPrizes) { this.customPrizes = customPrizes; }
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

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTeamType() { return teamType; }
    public void setTeamType(String teamType) { this.teamType = teamType; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }
    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public Boolean getJudgeScoringEnabled() { return judgeScoringEnabled; }
    public void setJudgeScoringEnabled(Boolean judgeScoringEnabled) { this.judgeScoringEnabled = judgeScoringEnabled; }
    
    public Double getPrize1Percent() { return prize1Percent; }
    public void setPrize1Percent(Double prize1Percent) { this.prize1Percent = prize1Percent; }
    public Double getPrize2Percent() { return prize2Percent; }
    public void setPrize2Percent(Double prize2Percent) { this.prize2Percent = prize2Percent; }
    public Double getPrize3Percent() { return prize3Percent; }
    public void setPrize3Percent(Double prize3Percent) { this.prize3Percent = prize3Percent; }

    public List<User> getJudges() { return judges; }
    public void setJudges(List<User> judges) { this.judges = judges; }

    @jakarta.persistence.Transient
    public Double getTotalPrizePool() {
        double fee = entryFee != null ? entryFee : 0.0;
        int count = maxParticipants != null && maxParticipants > 0 ? maxParticipants : (participants != null ? participants.size() : 0);
        if (count <= 0) count = 2;
        double total = fee * count;
        double comm = adminCommissionPercent != null ? adminCommissionPercent : 0.0;
        double pool = total * (1.0 - (comm / 100.0));
        return Math.round(pool * 100.0) / 100.0;
    }

    @jakarta.persistence.Transient
    public Double getPrize1Amount() {
        double pool = getTotalPrizePool();
        double pct = prize1Percent != null ? prize1Percent : 0.0;
        return Math.round(pool * (pct / 100.0) * 100.0) / 100.0;
    }

    @jakarta.persistence.Transient
    public Double getPrize2Amount() {
        double pool = getTotalPrizePool();
        double pct = prize2Percent != null ? prize2Percent : 0.0;
        return Math.round(pool * (pct / 100.0) * 100.0) / 100.0;
    }

    @jakarta.persistence.Transient
    public Double getPrize3Amount() {
        double pool = getTotalPrizePool();
        double pct = prize3Percent != null ? prize3Percent : 0.0;
        return Math.round(pool * (pct / 100.0) * 100.0) / 100.0;
    }
}

