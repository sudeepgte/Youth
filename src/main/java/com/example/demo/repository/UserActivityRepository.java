package com.example.demo.repository;

import com.example.demo.model.ActivityType;
import com.example.demo.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    List<UserActivity> findByUserIdAndPostId(Long userId, Long postId);

    List<UserActivity> findByPostIdAndActivityType(Long postId, ActivityType activityType);

    List<UserActivity> findByUserId(Long userId);

    /** All activities for any post — used to compute relationship score */
    List<UserActivity> findByUserIdAndTimestampAfter(Long userId, LocalDateTime since);

    /** Aggregate likes for a post */
    long countByPostIdAndActivityType(Long postId, ActivityType activityType);

    /** Sum watch-time for a post */
    @Query("SELECT COALESCE(SUM(a.watchTime), 0) FROM UserActivity a WHERE a.post.id = :postId AND a.activityType = 'VIEW'")
    long sumWatchTimeByPostId(Long postId);
}
