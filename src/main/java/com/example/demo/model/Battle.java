package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "battles")
public class Battle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String category; // Coding, Music, Dance, Photography, etc.

    @Column(length = 6, unique = true)
    private String roomCode;

    // WAITING, ACTIVE, VOTING, COMPLETED
    private String status = "WAITING";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id")
    private User creator;

    private Integer maxParticipants = 2;
    private Integer durationHours = 24;

    private String votingType = "PUBLIC";
    private String winnerLogic = "HIGHEST_VOTES";

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endsAt;
    private LocalDateTime votingEndsAt;

    // Winner fields
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner_id")
    private User winner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner2_id")
    private User winner2;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner3_id")
    private User winner3;

    private Integer winnerXp = 500;

    private String mode = "ONLINE"; // ONLINE or OFFLINE
    private String venue;
    private String eventDate;
    private String eventTime;
    private Double entryFee = 0.0;
    private Double prize1 = 0.0;
    private Double prize2 = 0.0;
    private Double prize3 = 0.0;
    private Double judgeWeight = 70.0;
    private Double audienceWeight = 30.0;

    @JsonIgnore
    @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BattleParticipant> participants = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BattleSubmission> submissions = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BattleVote> votes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (roomCode == null) roomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public String getVotingType() { return votingType; }
    public void setVotingType(String votingType) { this.votingType = votingType; }

    public String getWinnerLogic() { return winnerLogic; }
    public void setWinnerLogic(String winnerLogic) { this.winnerLogic = winnerLogic; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }

    public LocalDateTime getVotingEndsAt() { return votingEndsAt; }
    public void setVotingEndsAt(LocalDateTime votingEndsAt) { this.votingEndsAt = votingEndsAt; }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }

    public Integer getWinnerXp() { return winnerXp; }
    public void setWinnerXp(Integer winnerXp) { this.winnerXp = winnerXp; }

    public List<BattleParticipant> getParticipants() { return participants; }
    public void setParticipants(List<BattleParticipant> participants) { this.participants = participants; }

    public List<BattleSubmission> getSubmissions() { return submissions; }
    public void setSubmissions(List<BattleSubmission> submissions) { this.submissions = submissions; }

    public List<BattleVote> getVotes() { return votes; }
    public void setVotes(List<BattleVote> votes) { this.votes = votes; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public Double getEntryFee() { return entryFee; }
    public void setEntryFee(Double entryFee) { this.entryFee = entryFee; }

    public Double getJudgeWeight() { return judgeWeight; }
    public void setJudgeWeight(Double judgeWeight) { this.judgeWeight = judgeWeight; }

    public Double getAudienceWeight() { return audienceWeight; }
    public void setAudienceWeight(Double audienceWeight) { this.audienceWeight = audienceWeight; }

    public User getWinner2() { return winner2; }
    public void setWinner2(User winner2) { this.winner2 = winner2; }

    public User getWinner3() { return winner3; }
    public void setWinner3(User winner3) { this.winner3 = winner3; }

    public Double getPrize1() { return prize1; }
    public void setPrize1(Double prize1) { this.prize1 = prize1; }

    public Double getPrize2() { return prize2; }
    public void setPrize2(Double prize2) { this.prize2 = prize2; }

    public Double getPrize3() { return prize3; }
    public void setPrize3(Double prize3) { this.prize3 = prize3; }
}
