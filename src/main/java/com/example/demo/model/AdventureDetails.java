package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adventure_details")
public class AdventureDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "event_id")
    private Event event;

    private String adventureType;
    private String difficultyLevel;
    private String adventureDuration;
    private Double distanceCovered;
    private String elevation;
    private String fitnessLevelRequired;
    private Integer minAge;
    private Integer maxAge;
    
    @Column(columnDefinition = "TEXT")
    private String safetyEquipment;
    
    @Column(columnDefinition = "TEXT")
    private String thingsToBring;
    
    private Boolean medicalCertificateRequired;
    private Boolean professionalGuideAvailable;
    private Boolean insuranceIncluded;
    private Boolean emergencyRescueSupport;
    private String foodIncluded;
    private String stayIncluded;
    private Boolean transportationIncluded;
    private Boolean photographyIncluded;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AdventureDetails() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getAdventureType() { return adventureType; }
    public void setAdventureType(String adventureType) { this.adventureType = adventureType; }

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getAdventureDuration() { return adventureDuration; }
    public void setAdventureDuration(String adventureDuration) { this.adventureDuration = adventureDuration; }

    public Double getDistanceCovered() { return distanceCovered; }
    public void setDistanceCovered(Double distanceCovered) { this.distanceCovered = distanceCovered; }

    public String getElevation() { return elevation; }
    public void setElevation(String elevation) { this.elevation = elevation; }

    public String getFitnessLevelRequired() { return fitnessLevelRequired; }
    public void setFitnessLevelRequired(String fitnessLevelRequired) { this.fitnessLevelRequired = fitnessLevelRequired; }

    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }

    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    public String getSafetyEquipment() { return safetyEquipment; }
    public void setSafetyEquipment(String safetyEquipment) { this.safetyEquipment = safetyEquipment; }

    public String getThingsToBring() { return thingsToBring; }
    public void setThingsToBring(String thingsToBring) { this.thingsToBring = thingsToBring; }

    public Boolean getMedicalCertificateRequired() { return medicalCertificateRequired; }
    public void setMedicalCertificateRequired(Boolean medicalCertificateRequired) { this.medicalCertificateRequired = medicalCertificateRequired; }

    public Boolean getProfessionalGuideAvailable() { return professionalGuideAvailable; }
    public void setProfessionalGuideAvailable(Boolean professionalGuideAvailable) { this.professionalGuideAvailable = professionalGuideAvailable; }

    public Boolean getInsuranceIncluded() { return insuranceIncluded; }
    public void setInsuranceIncluded(Boolean insuranceIncluded) { this.insuranceIncluded = insuranceIncluded; }

    public Boolean getEmergencyRescueSupport() { return emergencyRescueSupport; }
    public void setEmergencyRescueSupport(Boolean emergencyRescueSupport) { this.emergencyRescueSupport = emergencyRescueSupport; }

    public String getFoodIncluded() { return foodIncluded; }
    public void setFoodIncluded(String foodIncluded) { this.foodIncluded = foodIncluded; }

    public String getStayIncluded() { return stayIncluded; }
    public void setStayIncluded(String stayIncluded) { this.stayIncluded = stayIncluded; }

    public Boolean getTransportationIncluded() { return transportationIncluded; }
    public void setTransportationIncluded(Boolean transportationIncluded) { this.transportationIncluded = transportationIncluded; }

    public Boolean getPhotographyIncluded() { return photographyIncluded; }
    public void setPhotographyIncluded(Boolean photographyIncluded) { this.photographyIncluded = photographyIncluded; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void setLastUpdate() { this.updatedAt = LocalDateTime.now(); }
}
