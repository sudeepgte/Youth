package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PostCollaboration> collaborations = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PostLike> likes = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PostComment> comments = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<UserActivity> userActivities = new java.util.ArrayList<>();

    private LocalDateTime createdAt;

    private String mediaUrl;
    private String mediaType; // "IMAGE" or "VIDEO"
    private String hashtags;
    private String postType; // "POST", "REEL", "STORY"
    private String category; // e.g. "Food", "Dance", "Vlog" etc.

    @Column(name = "comments_disabled", nullable = false)
    private boolean commentsDisabled = false;

    public Post() {
        this.createdAt = LocalDateTime.now();
    }

    public Post(String content, User user, String mediaUrl, String mediaType, String hashtags) {
        this.content = content;
        this.user = user;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.hashtags = hashtags;
        this.postType = "POST";
        this.createdAt = LocalDateTime.now();
    }

    public Post(String content, User user, String mediaUrl, String mediaType, String hashtags, String postType,
            String category) {
        this.content = content;
        this.user = user;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.hashtags = hashtags;
        this.postType = postType != null ? postType : "POST";
        this.category = category;
        this.createdAt = LocalDateTime.now();
    }

    public Post(String content, User user) {
        this.content = content;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isCommentsDisabled() {
        return commentsDisabled;
    }

    public void setCommentsDisabled(boolean commentsDisabled) {
        this.commentsDisabled = commentsDisabled;
    }

    public java.util.List<PostCollaboration> getCollaborations() {

        return collaborations;
    }

    public void setCollaborations(java.util.List<PostCollaboration> collaborations) {
        this.collaborations = collaborations;
    }

    public java.util.List<PostLike> getLikes() {
        return likes;
    }

    public void setLikes(java.util.List<PostLike> likes) {
        this.likes = likes;
    }

    public java.util.List<PostComment> getComments() {
        return comments;
    }

    public void setComments(java.util.List<PostComment> comments) {
        this.comments = comments;
    }

    public java.util.List<UserActivity> getUserActivities() {
        return userActivities;
    }

    public void setUserActivities(java.util.List<UserActivity> userActivities) {
        this.userActivities = userActivities;
    }

    public boolean isLikedByUser(User user) {
        if (user == null || likes == null) {
            return false;
        }
        return likes.stream().anyMatch(l -> l.getUser() != null && l.getUser().getId().equals(user.getId()));
    }

    private boolean blocked = false;
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
}
