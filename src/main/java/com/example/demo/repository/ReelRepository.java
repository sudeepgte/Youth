package com.example.demo.repository;

import com.example.demo.model.Reel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReelRepository extends JpaRepository<Reel, Long> {

    // For regular users (only see approved reels)
    Page<Reel> findByIsApprovedTrue(Pageable pageable);

    boolean existsByVideoUrl(String videoUrl);

    // For admin or specific user profile lists
    Page<Reel> findByUserId(Long userId, Pageable pageable);
}
