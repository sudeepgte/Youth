package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    @JsonIgnore
    @Column(unique = true)
    private String email;
    @JsonIgnore
    private String password;
    private boolean emailVerified = false;
    private LocalDate dob;
    private String gender;
    private String profilePicture;
    private java.time.LocalDateTime lastActiveAt;

    @Column(length = 300)
    private String bio;

    private String profilePhotoUrl;
    @Column(length = 1000)
    private String aboutMe;
    private String skills;
    private String collegeName;
    private boolean privateAccount = false;

    // Gamification & Ranking
    private Integer xp = 0;
    private String level = "Novice"; // Novice, Bronze, Silver, Gold, Platinum

    @JsonIgnore
    @ManyToMany
    @JoinTable(name = "user_followers", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "follower_id"))
    private Set<User> followers = new HashSet<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "followers")
    private Set<User> following = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_voted_events", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "event_id")
    private Set<Long> votedEvents = new HashSet<>();

    // Zentrix Coins & Rewards
    private Integer coins = 0;
    
    @Column(name = "wallet_balance")
    private Double walletBalance = 0.0;

    private LocalDate lastLoginDate;

    // Music reward bookkeeping (Option C MVP)
    private LocalDate lastMusicRewardDate;
    private LocalDate musicSecondsDate;
    private Integer musicRewardedSecondsToday = 0;
    
    // Premium Features
    private boolean isPremium = false;
    private java.time.LocalDateTime profileBoostUntil;
    private boolean hasDiscount = false;
    private boolean hasFreeEntry = false;

    public User() {
    }

    public User(String username, String email, String password, LocalDate dob, String gender, String profilePicture) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.dob = dob;
        this.gender = gender;
        this.profilePicture = profilePicture;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonIgnore
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public java.time.LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(java.time.LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public boolean isPrivateAccount() {
        return privateAccount;
    }

    public void setPrivateAccount(boolean privateAccount) {
        this.privateAccount = privateAccount;
    }

    @JsonIgnore
    public Set<User> getFollowers() {
        return followers;
    }

    public void setFollowers(Set<User> followers) {
        this.followers = followers;
    }

    @JsonIgnore
    public Set<User> getFollowing() {
        return following;
    }

    public void setFollowing(Set<User> following) {
        this.following = following;
    }

    public Integer getXp() {
        return xp;
    }

    public void setXp(Integer xp) {
        this.xp = xp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User))
            return false;
        User user = (User) o;
        return this.getId() != null && this.getId().equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 31;
    }

    public Set<Long> getVotedEvents() {
        return votedEvents;
    }

    public void setVotedEvents(Set<Long> votedEvents) {
        this.votedEvents = votedEvents;
    }

    // Zentrix Coins & Rewards Getters/Setters
    public Integer getCoins() { return coins != null ? coins : 0; }
    public void setCoins(Integer coins) { this.coins = coins; }
    public void addCoins(int amount) { this.coins = getCoins() + amount; }

    public Double getWalletBalance() { return walletBalance != null ? walletBalance : 0.0; }
    public void setWalletBalance(Double walletBalance) { this.walletBalance = walletBalance; }
    public void addWalletBalance(Double amount) {
        if (this.walletBalance == null) this.walletBalance = 0.0;
        this.walletBalance += amount;
    }
    public boolean deductWalletBalance(Double amount) {
        if (this.walletBalance == null) this.walletBalance = 0.0;
        if (this.walletBalance >= amount) {
            this.walletBalance -= amount;
            return true;
        }
        return false;
    }

    public LocalDate getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(LocalDate lastLoginDate) { this.lastLoginDate = lastLoginDate; }

    public LocalDate getLastMusicRewardDate() {
        return lastMusicRewardDate;
    }

    public void setLastMusicRewardDate(LocalDate lastMusicRewardDate) {
        this.lastMusicRewardDate = lastMusicRewardDate;
    }

    public LocalDate getMusicSecondsDate() {
        return musicSecondsDate;
    }

    public void setMusicSecondsDate(LocalDate musicSecondsDate) {
        this.musicSecondsDate = musicSecondsDate;
    }

    public Integer getMusicRewardedSecondsToday() {
        return musicRewardedSecondsToday != null ? musicRewardedSecondsToday : 0;
    }

    public void setMusicRewardedSecondsToday(Integer musicRewardedSecondsToday) {
        this.musicRewardedSecondsToday = musicRewardedSecondsToday;
    }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean isPremium) { this.isPremium = isPremium; }

    public java.time.LocalDateTime getProfileBoostUntil() { return profileBoostUntil; }
    public void setProfileBoostUntil(java.time.LocalDateTime profileBoostUntil) { this.profileBoostUntil = profileBoostUntil; }

    public boolean isProfileBoosted() {
        return profileBoostUntil != null && profileBoostUntil.isAfter(java.time.LocalDateTime.now());
    }

    public boolean isHasDiscount() { return hasDiscount; }
    public void setHasDiscount(boolean hasDiscount) { this.hasDiscount = hasDiscount; }

    public boolean isHasFreeEntry() { return hasFreeEntry; }
    public void setHasFreeEntry(boolean hasFreeEntry) { this.hasFreeEntry = hasFreeEntry; }
}
