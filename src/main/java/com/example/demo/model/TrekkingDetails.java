package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trekking_details")
public class TrekkingDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "event_id")
    private Event event;

    private String trekType;
    private String trekDifficulty;
    private Double trekDistance;
    private String estimatedDuration;
    private String maxElevation;
    private String trailType;
    private String fitnessLevel;
    private Integer minAge;
    private Integer maxAge;
    private String reportingPoint;
    private String reportingTime;
    private String departureTime;
    private String returnTime;
    
    @Column(columnDefinition = "TEXT")
    private String trekInclusions;
    
    @Column(columnDefinition = "TEXT")
    private String participantsMustCarry;
    
    private Boolean medicalCertificateRequired;
    private Boolean emergencyRescueSupport;
    private Boolean forestPermissionRequired;
    private String mobileNetworkAvailability;
    private Boolean washroomFacility;
    private Boolean drinkingWaterAvailability;
    
    @Column(columnDefinition = "TEXT")
    private String trekSchedule;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public TrekkingDetails() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getTrekType() { return trekType; }
    public void setTrekType(String trekType) { this.trekType = trekType; }

    public String getTrekDifficulty() { return trekDifficulty; }
    public void setTrekDifficulty(String trekDifficulty) { this.trekDifficulty = trekDifficulty; }

    public Double getTrekDistance() { return trekDistance; }
    public void setTrekDistance(Double trekDistance) { this.trekDistance = trekDistance; }

    public String getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(String estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public String getMaxElevation() { return maxElevation; }
    public void setMaxElevation(String maxElevation) { this.maxElevation = maxElevation; }

    public String getTrailType() { return trailType; }
    public void setTrailType(String trailType) { this.trailType = trailType; }

    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }

    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }

    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    public String getReportingPoint() { return reportingPoint; }
    public void setReportingPoint(String reportingPoint) { this.reportingPoint = reportingPoint; }

    public String getReportingTime() { return reportingTime; }
    public void setReportingTime(String reportingTime) { this.reportingTime = reportingTime; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getReturnTime() { return returnTime; }
    public void setReturnTime(String returnTime) { this.returnTime = returnTime; }

    public String getTrekInclusions() { return trekInclusions; }
    public void setTrekInclusions(String trekInclusions) { this.trekInclusions = trekInclusions; }

    public String getParticipantsMustCarry() { return participantsMustCarry; }
    public void setParticipantsMustCarry(String participantsMustCarry) { this.participantsMustCarry = participantsMustCarry; }

    public Boolean getMedicalCertificateRequired() { return medicalCertificateRequired; }
    public void setMedicalCertificateRequired(Boolean medicalCertificateRequired) { this.medicalCertificateRequired = medicalCertificateRequired; }

    public Boolean getEmergencyRescueSupport() { return emergencyRescueSupport; }
    public void setEmergencyRescueSupport(Boolean emergencyRescueSupport) { this.emergencyRescueSupport = emergencyRescueSupport; }

    public Boolean getForestPermissionRequired() { return forestPermissionRequired; }
    public void setForestPermissionRequired(Boolean forestPermissionRequired) { this.forestPermissionRequired = forestPermissionRequired; }

    public String getMobileNetworkAvailability() { return mobileNetworkAvailability; }
    public void setMobileNetworkAvailability(String mobileNetworkAvailability) { this.mobileNetworkAvailability = mobileNetworkAvailability; }

    public Boolean getWashroomFacility() { return washroomFacility; }
    public void setWashroomFacility(Boolean washroomFacility) { this.washroomFacility = washroomFacility; }

    public Boolean getDrinkingWaterAvailability() { return drinkingWaterAvailability; }
    public void setDrinkingWaterAvailability(Boolean drinkingWaterAvailability) { this.drinkingWaterAvailability = drinkingWaterAvailability; }

    public String getTrekSchedule() { return trekSchedule; }
    public void setTrekSchedule(String trekSchedule) { this.trekSchedule = trekSchedule; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void setLastUpdate() { this.updatedAt = LocalDateTime.now(); }
}
