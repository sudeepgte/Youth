package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bike_riding_details")
public class BikeRidingDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "event_id")
    private Event event;

    private String rideType;
    
    @Column(columnDefinition = "TEXT")
    private String bikeTypeAllowed;
    
    private String minEngineCapacity;
    private String ridingExperience;
    private String startPoint;
    private String destination;
    private Double totalDistance;
    private String estimatedDuration;
    private String reportingTime;
    private String rideStartTime;
    private String estimatedFinishTime;
    
    @Column(columnDefinition = "TEXT")
    private String safetyGearMandatory;
    
    @Column(columnDefinition = "TEXT")
    private String supportAvailable;
    
    @Column(columnDefinition = "TEXT")
    private String rideInclusions;
    
    @Column(columnDefinition = "TEXT")
    private String riderRequirements;
    
    private String fuelPolicy;
    private String rideDifficulty;
    private String roadType;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public BikeRidingDetails() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getRideType() { return rideType; }
    public void setRideType(String rideType) { this.rideType = rideType; }

    public String getBikeTypeAllowed() { return bikeTypeAllowed; }
    public void setBikeTypeAllowed(String bikeTypeAllowed) { this.bikeTypeAllowed = bikeTypeAllowed; }

    public String getMinEngineCapacity() { return minEngineCapacity; }
    public void setMinEngineCapacity(String minEngineCapacity) { this.minEngineCapacity = minEngineCapacity; }

    public String getRidingExperience() { return ridingExperience; }
    public void setRidingExperience(String ridingExperience) { this.ridingExperience = ridingExperience; }

    public String getStartPoint() { return startPoint; }
    public void setStartPoint(String startPoint) { this.startPoint = startPoint; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(Double totalDistance) { this.totalDistance = totalDistance; }

    public String getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(String estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public String getReportingTime() { return reportingTime; }
    public void setReportingTime(String reportingTime) { this.reportingTime = reportingTime; }

    public String getRideStartTime() { return rideStartTime; }
    public void setRideStartTime(String rideStartTime) { this.rideStartTime = rideStartTime; }

    public String getEstimatedFinishTime() { return estimatedFinishTime; }
    public void setEstimatedFinishTime(String estimatedFinishTime) { this.estimatedFinishTime = estimatedFinishTime; }

    public String getSafetyGearMandatory() { return safetyGearMandatory; }
    public void setSafetyGearMandatory(String safetyGearMandatory) { this.safetyGearMandatory = safetyGearMandatory; }

    public String getSupportAvailable() { return supportAvailable; }
    public void setSupportAvailable(String supportAvailable) { this.supportAvailable = supportAvailable; }

    public String getRideInclusions() { return rideInclusions; }
    public void setRideInclusions(String rideInclusions) { this.rideInclusions = rideInclusions; }

    public String getRiderRequirements() { return riderRequirements; }
    public void setRiderRequirements(String riderRequirements) { this.riderRequirements = riderRequirements; }

    public String getFuelPolicy() { return fuelPolicy; }
    public void setFuelPolicy(String fuelPolicy) { this.fuelPolicy = fuelPolicy; }

    public String getRideDifficulty() { return rideDifficulty; }
    public void setRideDifficulty(String rideDifficulty) { this.rideDifficulty = rideDifficulty; }

    public String getRoadType() { return roadType; }
    public void setRoadType(String roadType) { this.roadType = roadType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void setLastUpdate() { this.updatedAt = LocalDateTime.now(); }
}
