package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class SecretRewardPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private String businessName;
    private String category;
    private String rewardName;
    private Integer quantity;
    private Integer remainingQuantity;
    private Integer validityDays;
    private String description;

    private String redeemStallNumber;
    private String storeName;
    private String storeAddress;
    private String storeContact;
    private String couponCode;
    
    private String deliveryMethod;
    private String estimatedDelivery;
    private String deliveryType;

    @Column(columnDefinition = "TEXT")
    private String terms;

    private String sponsorLogoUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRewardName() { return rewardName; }
    public void setRewardName(String rewardName) { this.rewardName = rewardName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }
    public String getSponsorLogoUrl() { return sponsorLogoUrl; }
    public void setSponsorLogoUrl(String sponsorLogoUrl) { this.sponsorLogoUrl = sponsorLogoUrl; }
    
    public String getRedeemStallNumber() { return redeemStallNumber; }
    public void setRedeemStallNumber(String redeemStallNumber) { this.redeemStallNumber = redeemStallNumber; }
    
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    
    public String getStoreAddress() { return storeAddress; }
    public void setStoreAddress(String storeAddress) { this.storeAddress = storeAddress; }
    
    public String getStoreContact() { return storeContact; }
    public void setStoreContact(String storeContact) { this.storeContact = storeContact; }
    
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    
    public String getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(String estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
    
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
}
