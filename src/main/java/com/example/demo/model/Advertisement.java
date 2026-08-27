package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "advertisements")
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String organizationName;

    @Enumerated(EnumType.STRING)
    private AdType adType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;
    private String ctaText;
    private String ctaUrl;

    @Enumerated(EnumType.STRING)
    private AdPlacement placement;

    @Enumerated(EnumType.STRING)
    private TargetAudience targetAudience;

    private String targetCollege;
    private String targetCity;

    private int priority = 0;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    private AdStatus status = AdStatus.DRAFT;

    private Long maxImpressions;
    private Long maxClicks;

    private long impressionCount = 0;
    private long clickCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public AdType getAdType() { return adType; }
    public void setAdType(AdType adType) { this.adType = adType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCtaText() { return ctaText; }
    public void setCtaText(String ctaText) { this.ctaText = ctaText; }
    public String getCtaUrl() { return ctaUrl; }
    public void setCtaUrl(String ctaUrl) { this.ctaUrl = ctaUrl; }
    public AdPlacement getPlacement() { return placement; }
    public void setPlacement(AdPlacement placement) { this.placement = placement; }
    public TargetAudience getTargetAudience() { return targetAudience; }
    public void setTargetAudience(TargetAudience targetAudience) { this.targetAudience = targetAudience; }
    public String getTargetCollege() { return targetCollege; }
    public void setTargetCollege(String targetCollege) { this.targetCollege = targetCollege; }
    public String getTargetCity() { return targetCity; }
    public void setTargetCity(String targetCity) { this.targetCity = targetCity; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public AdStatus getStatus() { return status; }
    public void setStatus(AdStatus status) { this.status = status; }
    public Long getMaxImpressions() { return maxImpressions; }
    public void setMaxImpressions(Long maxImpressions) { this.maxImpressions = maxImpressions; }
    public Long getMaxClicks() { return maxClicks; }
    public void setMaxClicks(Long maxClicks) { this.maxClicks = maxClicks; }
    public long getImpressionCount() { return impressionCount; }
    public void setImpressionCount(long impressionCount) { this.impressionCount = impressionCount; }
    public long getClickCount() { return clickCount; }
    public void setClickCount(long clickCount) { this.clickCount = clickCount; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
