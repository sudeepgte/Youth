package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;
    private LocalDateTime dateTime;
    private String venue;
    private String price;
    private String category;
    private String organizer;
    private LocalDateTime createdAt = LocalDateTime.now();

    // New fields for admin create event flow
    private String status = "UPCOMING";  // UPCOMING, ONGOING, COMPLETED
    private Integer maxParticipants;
    private String entryFeeType = "Free"; // Free or Paid
    private String eventMode = "Offline"; // Offline or Online
    private String meetingLink;           // For online events

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<EventSeatTier> seatTiers = new ArrayList<>();

    // NEW: Secret Rewards
    private boolean enableSecretRewards = false;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<SecretRewardPartner> secretRewards = new ArrayList<>();

    // NEW: Individual Seat Grid Selection (BMOS Style)
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventSeat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventRegistration> registrations = new ArrayList<>();
    
    // Administrative grid setup
    private Integer totalRows = 0;      // E.g. 10 (A-J)
    private Integer seatsPerRow = 0;    // E.g. 20
    private String seatLayoutType = "Standard"; // Standard, Auditorium, Hall
    
    // Voting / Poll Feature
    private LocalDateTime votingStartDate;
    private LocalDateTime votingEndDate;
    private Integer pollVotes = 0;
    private String votingStatus = "OPEN"; // OPEN, CLOSED

    // Simplified Seat Setup
    private Integer vipSeatCount = 0;
    private Double vipPrice = 0.0;
    private Double regularPrice = 0.0;

    // Final Voting Phase Option
    private boolean finalVotingEnabled = false;

    public Event() {
    }

    public Integer getVipSeatCount() { return vipSeatCount; }
    public void setVipSeatCount(Integer vipSeatCount) { this.vipSeatCount = vipSeatCount; }

    public Double getVipPrice() { return vipPrice; }
    public void setVipPrice(Double vipPrice) { this.vipPrice = vipPrice; }

    public Double getRegularPrice() { return regularPrice; }
    public void setRegularPrice(Double regularPrice) { this.regularPrice = regularPrice; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getEntryFeeType() { return entryFeeType; }
    public void setEntryFeeType(String entryFeeType) { this.entryFeeType = entryFeeType; }

    public String getEventMode() { return eventMode; }
    public void setEventMode(String eventMode) { this.eventMode = eventMode; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public List<EventSeatTier> getSeatTiers() { return seatTiers; }
    public void setSeatTiers(List<EventSeatTier> seatTiers) { 
        if (seatTiers == null) {
            this.seatTiers.clear();
        } else {
            this.seatTiers.clear();
            this.seatTiers.addAll(seatTiers);
            for (EventSeatTier tier : seatTiers) {
                tier.setEvent(this);
            }
        }
    }

    public boolean isEnableSecretRewards() { return enableSecretRewards; }
    public void setEnableSecretRewards(boolean enableSecretRewards) { this.enableSecretRewards = enableSecretRewards; }

    public List<SecretRewardPartner> getSecretRewards() { return secretRewards; }
    public void setSecretRewards(List<SecretRewardPartner> secretRewards) {
        if (secretRewards == null) {
            this.secretRewards.clear();
        } else {
            this.secretRewards.clear();
            this.secretRewards.addAll(secretRewards);
            for (SecretRewardPartner reward : secretRewards) {
                reward.setEvent(this);
            }
        }
    }

    public LocalDateTime getVotingStartDate() { return votingStartDate; }
    public void setVotingStartDate(LocalDateTime votingStartDate) { this.votingStartDate = votingStartDate; }

    public LocalDateTime getVotingEndDate() { return votingEndDate; }
    public void setVotingEndDate(LocalDateTime votingEndDate) { this.votingEndDate = votingEndDate; }

    public Integer getPollVotes() { return pollVotes != null ? pollVotes : 0; }
    public void setPollVotes(Integer pollVotes) { this.pollVotes = pollVotes; }

    public List<EventSeat> getSeats() { return seats; }
    public void setSeats(List<EventSeat> seats) { this.seats = seats; }

    public Integer getTotalRows() { return totalRows != null ? totalRows : 0; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public Integer getSeatsPerRow() { return seatsPerRow != null ? seatsPerRow : 0; }
    public void setSeatsPerRow(Integer seatsPerRow) { this.seatsPerRow = seatsPerRow; }

    public String getSeatLayoutType() { return seatLayoutType; }
    public void setSeatLayoutType(String seatLayoutType) { this.seatLayoutType = seatLayoutType; }

    public String getVotingStatus() { return votingStatus; }
    public void setVotingStatus(String votingStatus) { this.votingStatus = votingStatus; }

    public boolean isFinalVotingEnabled() { return finalVotingEnabled; }
    public void setFinalVotingEnabled(boolean finalVotingEnabled) { this.finalVotingEnabled = finalVotingEnabled; }

    public List<EventRegistration> getRegistrations() { return registrations; }
    public void setRegistrations(List<EventRegistration> registrations) { this.registrations = registrations; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
