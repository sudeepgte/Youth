package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post_comments")
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 1000, nullable = false, columnDefinition = "VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @JsonIgnore
    private PostComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<PostComment> replies = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    public PostComment() {
    }

    public PostComment(Post post, User user, String content) {
        this.post = post;
        this.user = user;
        this.content = content;
    }

    public PostComment(Post post, User user, String content, PostComment parentComment) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.parentComment = parentComment;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post p) {
        this.post = p;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User u) {
        this.user = u;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String c) {
        this.content = c;
    }

    public PostComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(PostComment parentComment) {
        this.parentComment = parentComment;
    }

    public List<PostComment> getReplies() {
        return replies;
    }

    public void setReplies(List<PostComment> replies) {
        this.replies = replies;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }
}
