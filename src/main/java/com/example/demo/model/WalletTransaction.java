package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Double amount;

    private String currency = "INR"; // Default to INR to support old calls

    private String type; // "CREDIT" or "DEBIT"

    private String description;

    private String referenceId;

    private LocalDateTime timestamp = LocalDateTime.now();

    public WalletTransaction() {}

    public WalletTransaction(User user, Double amount, String type, String description) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDetails() { return description; }
    public void setDetails(String details) { this.description = details; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    // Alias for createdAt if anything else expects it
    public LocalDateTime getCreatedAt() { return timestamp; }
    public void setCreatedAt(LocalDateTime createdAt) { this.timestamp = createdAt; }
}