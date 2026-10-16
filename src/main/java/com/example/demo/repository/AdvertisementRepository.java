package com.example.demo.repository;

import com.example.demo.model.Advertisement;
import com.example.demo.model.AdPlacement;
import com.example.demo.model.AdStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    List<Advertisement> findAllByOrderByCreatedAtDesc();
    
    // For finding ads by placement that might be active
    List<Advertisement> findByPlacementAndStatusInOrderByPriorityDesc(AdPlacement placement, List<AdStatus> statuses);
    
    long countByStatus(AdStatus status);
    
    @Query("SELECT SUM(a.impressionCount) FROM Advertisement a")
    Long sumTotalImpressions();
    
    @Query("SELECT SUM(a.clickCount) FROM Advertisement a")
    Long sumTotalClicks();
}
