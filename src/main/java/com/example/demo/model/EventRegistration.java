package com.example.demo.model;

import com.example.demo.model.Event;
import com.example.demo.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations")
public class EventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String selectedTier; // VIP, GOLD, etc.

    private LocalDateTime registrationDate;

    // Ticket & Payment fields
    @Column(unique = true)
    private String ticketId;

    private String registrationStatus = "REGISTERED"; // REGISTERED, CANCELLED

    private String paymentStatus = "FREE"; // FREE, PAID, PENDING
    
    private Integer quantity = 1;
    
    private Double totalPrice = 0.0;

    // User-provided registration info
    private String fullName;
    private String email;
    private String phone;
    private String college;
    private String yearOfStudy;

    // Attendance tracking
    private boolean attendanceMarked = false;
    private LocalDateTime attendedAt;

    // Secret Rewards tracking
    private boolean rewardRevealed = false;

    // Results & Ranking
    private String position; // "Winner", "Runner", "Participant"
    private Integer pointsEarned = 0;

    // Secret Voting System
    private Double judgeScore = 0.0;
    private Integer publicVotes = 0;
    private Double finalScore = 0.0;

    // Finalist Top 3 feature
    private boolean isFinalist = false;
    private String finalistMediaUrl;
    @Column(columnDefinition = "TEXT")
    private String finalistDescription;

    public EventRegistration() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getRegistrationStatus() { return registrationStatus; }
    public void setRegistrationStatus(String registrationStatus) { this.registrationStatus = registrationStatus; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public boolean isAttendanceMarked() { return attendanceMarked; }
    public void setAttendanceMarked(boolean attendanceMarked) { this.attendanceMarked = attendanceMarked; }

    public boolean isRewardRevealed() { return rewardRevealed; }
    public void setRewardRevealed(boolean rewardRevealed) { this.rewardRevealed = rewardRevealed; }

    public LocalDateTime getAttendedAt() { return attendedAt; }
    public void setAttendedAt(LocalDateTime attendedAt) { this.attendedAt = attendedAt; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }

    public Double getJudgeScore() { return judgeScore; }
    public void setJudgeScore(Double judgeScore) { this.judgeScore = judgeScore; }

    public Integer getPublicVotes() { return publicVotes; }
    public void setPublicVotes(Integer publicVotes) { this.publicVotes = publicVotes; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getSelectedTier() { return selectedTier; }
    public void setSelectedTier(String selectedTier) { this.selectedTier = selectedTier; }

    public boolean isFinalist() { return isFinalist; }
    public void setFinalist(boolean finalist) { isFinalist = finalist; }

    public String getFinalistMediaUrl() { return finalistMediaUrl; }
    public void setFinalistMediaUrl(String finalistMediaUrl) { this.finalistMediaUrl = finalistMediaUrl; }

    public String getFinalistDescription() { return finalistDescription; }
    public void setFinalistDescription(String finalistDescription) { this.finalistDescription = finalistDescription; }

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
    }
}
