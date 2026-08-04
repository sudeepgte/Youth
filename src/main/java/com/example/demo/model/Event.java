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
    
    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

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

    // Heatmap Geolocation
    private Double latitude;
    private Double longitude;

    public Event() {
    }

    public Integer getVipSeatCount() { return vipSeatCount; }
    public void setVipSeatCount(Integer vipSeatCount) { this.vipSeatCount = vipSeatCount; }

    public Double getVipPrice() { return vipPrice; }
    public void setVipPrice(Double vipPrice) { this.vipPrice = vipPrice; }

    public Double getRegularPrice() { return regularPrice; }
    public void setRegularPrice(Double regularPrice) { this.regularPrice = regularPrice; }


    // House Parties
    private String hpPartyType;
    private String hpTheme;
    private String hpDressCode;
    private Integer hpGuestLimit;
    private String hpEntryType;
    private Integer hpMinimumAge;
    private Integer hpMaximumAge;
    private String hpMusicGenre;
    private boolean hpFoodIncluded;
    private boolean hpDrinksIncluded;
    private boolean hpParkingAvailable;
    private boolean hpSecurityAvailable;

    // Trekking
    private String trTrekDifficulty;
    private Double trTrekDistance;
    private String trTrekDuration;
    private String trTrekHeight;
    private String trMeetingPoint;
    private String trReportingTime;
    private String trFitnessLevel;
    private boolean trGuideIncluded;
    private boolean trCampingIncluded;
    private boolean trFoodIncluded;
    private boolean trTransportIncluded;
    private String trEmergencyContact;
    private String trRequiredEquipment;
    private boolean trMedicalCertRequired;

    // Adventure
    private String advAdventureType;
    private String advDifficultyLevel;
    private boolean advSafetyEquipIncluded;
    private boolean advInstructorAvailable;
    private boolean advInsuranceIncluded;
    private boolean advMedicalCertRequired;
    private Integer advMinAge;
    private Integer advMaxAge;
    private String advDuration;
    private String advTiming;
    private String advEmergencyContact;

    // Biking
    private String bkRideType;
    private Double bkRideDistance;
    private String bkStartLocation;
    private String bkDestination;
    private String bkMeetingPoint;
    private String bkReportingTime;
    private String bkCCLimit;
    private boolean bkHelmetMandatory;
    private boolean bkJacketMandatory;
    private boolean bkGlovesMandatory;
    private boolean bkExperienceRequired;
    private boolean bkFuelIncluded;
    private boolean bkSupportVehicle;
    private boolean bkBreakfastIncluded;
    private boolean bkLunchIncluded;
    private boolean bkDinnerIncluded;
    private boolean bkStayIncluded;
    private String bkEmergencySupport;

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

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public List<EventRegistration> getRegistrations() { return registrations; }
    public void setRegistrations(List<EventRegistration> registrations) { this.registrations = registrations; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public String getHpPartyType() { return hpPartyType; }
    public void setHpPartyType(String hpPartyType) { this.hpPartyType = hpPartyType; }
    public String getHpTheme() { return hpTheme; }
    public void setHpTheme(String hpTheme) { this.hpTheme = hpTheme; }
    public String getHpDressCode() { return hpDressCode; }
    public void setHpDressCode(String hpDressCode) { this.hpDressCode = hpDressCode; }
    public Integer getHpGuestLimit() { return hpGuestLimit; }
    public void setHpGuestLimit(Integer hpGuestLimit) { this.hpGuestLimit = hpGuestLimit; }
    public String getHpEntryType() { return hpEntryType; }
    public void setHpEntryType(String hpEntryType) { this.hpEntryType = hpEntryType; }
    public Integer getHpMinimumAge() { return hpMinimumAge; }
    public void setHpMinimumAge(Integer hpMinimumAge) { this.hpMinimumAge = hpMinimumAge; }
    public Integer getHpMaximumAge() { return hpMaximumAge; }
    public void setHpMaximumAge(Integer hpMaximumAge) { this.hpMaximumAge = hpMaximumAge; }
    public String getHpMusicGenre() { return hpMusicGenre; }
    public void setHpMusicGenre(String hpMusicGenre) { this.hpMusicGenre = hpMusicGenre; }
    public boolean isHpFoodIncluded() { return hpFoodIncluded; }
    public void setHpFoodIncluded(boolean hpFoodIncluded) { this.hpFoodIncluded = hpFoodIncluded; }
    public boolean isHpDrinksIncluded() { return hpDrinksIncluded; }
    public void setHpDrinksIncluded(boolean hpDrinksIncluded) { this.hpDrinksIncluded = hpDrinksIncluded; }
    public boolean isHpParkingAvailable() { return hpParkingAvailable; }
    public void setHpParkingAvailable(boolean hpParkingAvailable) { this.hpParkingAvailable = hpParkingAvailable; }
    public boolean isHpSecurityAvailable() { return hpSecurityAvailable; }
    public void setHpSecurityAvailable(boolean hpSecurityAvailable) { this.hpSecurityAvailable = hpSecurityAvailable; }
    public String getTrTrekDifficulty() { return trTrekDifficulty; }
    public void setTrTrekDifficulty(String trTrekDifficulty) { this.trTrekDifficulty = trTrekDifficulty; }
    public Double getTrTrekDistance() { return trTrekDistance; }
    public void setTrTrekDistance(Double trTrekDistance) { this.trTrekDistance = trTrekDistance; }
    public String getTrTrekDuration() { return trTrekDuration; }
    public void setTrTrekDuration(String trTrekDuration) { this.trTrekDuration = trTrekDuration; }
    public String getTrTrekHeight() { return trTrekHeight; }
    public void setTrTrekHeight(String trTrekHeight) { this.trTrekHeight = trTrekHeight; }
    public String getTrMeetingPoint() { return trMeetingPoint; }
    public void setTrMeetingPoint(String trMeetingPoint) { this.trMeetingPoint = trMeetingPoint; }
    public String getTrReportingTime() { return trReportingTime; }
    public void setTrReportingTime(String trReportingTime) { this.trReportingTime = trReportingTime; }
    public String getTrFitnessLevel() { return trFitnessLevel; }
    public void setTrFitnessLevel(String trFitnessLevel) { this.trFitnessLevel = trFitnessLevel; }
    public boolean isTrGuideIncluded() { return trGuideIncluded; }
    public void setTrGuideIncluded(boolean trGuideIncluded) { this.trGuideIncluded = trGuideIncluded; }
    public boolean isTrCampingIncluded() { return trCampingIncluded; }
    public void setTrCampingIncluded(boolean trCampingIncluded) { this.trCampingIncluded = trCampingIncluded; }
    public boolean isTrFoodIncluded() { return trFoodIncluded; }
    public void setTrFoodIncluded(boolean trFoodIncluded) { this.trFoodIncluded = trFoodIncluded; }
    public boolean isTrTransportIncluded() { return trTransportIncluded; }
    public void setTrTransportIncluded(boolean trTransportIncluded) { this.trTransportIncluded = trTransportIncluded; }
    public String getTrEmergencyContact() { return trEmergencyContact; }
    public void setTrEmergencyContact(String trEmergencyContact) { this.trEmergencyContact = trEmergencyContact; }
    public String getTrRequiredEquipment() { return trRequiredEquipment; }
    public void setTrRequiredEquipment(String trRequiredEquipment) { this.trRequiredEquipment = trRequiredEquipment; }
    public boolean isTrMedicalCertRequired() { return trMedicalCertRequired; }
    public void setTrMedicalCertRequired(boolean trMedicalCertRequired) { this.trMedicalCertRequired = trMedicalCertRequired; }
    public String getAdvAdventureType() { return advAdventureType; }
    public void setAdvAdventureType(String advAdventureType) { this.advAdventureType = advAdventureType; }
    public String getAdvDifficultyLevel() { return advDifficultyLevel; }
    public void setAdvDifficultyLevel(String advDifficultyLevel) { this.advDifficultyLevel = advDifficultyLevel; }
    public boolean isAdvSafetyEquipIncluded() { return advSafetyEquipIncluded; }
    public void setAdvSafetyEquipIncluded(boolean advSafetyEquipIncluded) { this.advSafetyEquipIncluded = advSafetyEquipIncluded; }
    public boolean isAdvInstructorAvailable() { return advInstructorAvailable; }
    public void setAdvInstructorAvailable(boolean advInstructorAvailable) { this.advInstructorAvailable = advInstructorAvailable; }
    public boolean isAdvInsuranceIncluded() { return advInsuranceIncluded; }
    public void setAdvInsuranceIncluded(boolean advInsuranceIncluded) { this.advInsuranceIncluded = advInsuranceIncluded; }
    public boolean isAdvMedicalCertRequired() { return advMedicalCertRequired; }
    public void setAdvMedicalCertRequired(boolean advMedicalCertRequired) { this.advMedicalCertRequired = advMedicalCertRequired; }
    public Integer getAdvMinAge() { return advMinAge; }
    public void setAdvMinAge(Integer advMinAge) { this.advMinAge = advMinAge; }
    public Integer getAdvMaxAge() { return advMaxAge; }
    public void setAdvMaxAge(Integer advMaxAge) { this.advMaxAge = advMaxAge; }
    public String getAdvDuration() { return advDuration; }
    public void setAdvDuration(String advDuration) { this.advDuration = advDuration; }
    public String getAdvTiming() { return advTiming; }
    public void setAdvTiming(String advTiming) { this.advTiming = advTiming; }
    public String getAdvEmergencyContact() { return advEmergencyContact; }
    public void setAdvEmergencyContact(String advEmergencyContact) { this.advEmergencyContact = advEmergencyContact; }
    public String getBkRideType() { return bkRideType; }
    public void setBkRideType(String bkRideType) { this.bkRideType = bkRideType; }
    public Double getBkRideDistance() { return bkRideDistance; }
    public void setBkRideDistance(Double bkRideDistance) { this.bkRideDistance = bkRideDistance; }
    public String getBkStartLocation() { return bkStartLocation; }
    public void setBkStartLocation(String bkStartLocation) { this.bkStartLocation = bkStartLocation; }
    public String getBkDestination() { return bkDestination; }
    public void setBkDestination(String bkDestination) { this.bkDestination = bkDestination; }
    public String getBkMeetingPoint() { return bkMeetingPoint; }
    public void setBkMeetingPoint(String bkMeetingPoint) { this.bkMeetingPoint = bkMeetingPoint; }
    public String getBkReportingTime() { return bkReportingTime; }
    public void setBkReportingTime(String bkReportingTime) { this.bkReportingTime = bkReportingTime; }
    public String getBkCCLimit() { return bkCCLimit; }
    public void setBkCCLimit(String bkCCLimit) { this.bkCCLimit = bkCCLimit; }
    public boolean isBkHelmetMandatory() { return bkHelmetMandatory; }
    public void setBkHelmetMandatory(boolean bkHelmetMandatory) { this.bkHelmetMandatory = bkHelmetMandatory; }
    public boolean isBkJacketMandatory() { return bkJacketMandatory; }
    public void setBkJacketMandatory(boolean bkJacketMandatory) { this.bkJacketMandatory = bkJacketMandatory; }
    public boolean isBkGlovesMandatory() { return bkGlovesMandatory; }
    public void setBkGlovesMandatory(boolean bkGlovesMandatory) { this.bkGlovesMandatory = bkGlovesMandatory; }
    public boolean isBkExperienceRequired() { return bkExperienceRequired; }
    public void setBkExperienceRequired(boolean bkExperienceRequired) { this.bkExperienceRequired = bkExperienceRequired; }
    public boolean isBkFuelIncluded() { return bkFuelIncluded; }
    public void setBkFuelIncluded(boolean bkFuelIncluded) { this.bkFuelIncluded = bkFuelIncluded; }
    public boolean isBkSupportVehicle() { return bkSupportVehicle; }
    public void setBkSupportVehicle(boolean bkSupportVehicle) { this.bkSupportVehicle = bkSupportVehicle; }
    public boolean isBkBreakfastIncluded() { return bkBreakfastIncluded; }
    public void setBkBreakfastIncluded(boolean bkBreakfastIncluded) { this.bkBreakfastIncluded = bkBreakfastIncluded; }
    public boolean isBkLunchIncluded() { return bkLunchIncluded; }
    public void setBkLunchIncluded(boolean bkLunchIncluded) { this.bkLunchIncluded = bkLunchIncluded; }
    public boolean isBkDinnerIncluded() { return bkDinnerIncluded; }
    public void setBkDinnerIncluded(boolean bkDinnerIncluded) { this.bkDinnerIncluded = bkDinnerIncluded; }
    public boolean isBkStayIncluded() { return bkStayIncluded; }
    public void setBkStayIncluded(boolean bkStayIncluded) { this.bkStayIncluded = bkStayIncluded; }
    public String getBkEmergencySupport() { return bkEmergencySupport; }
    public void setBkEmergencySupport(String bkEmergencySupport) { this.bkEmergencySupport = bkEmergencySupport; }

}