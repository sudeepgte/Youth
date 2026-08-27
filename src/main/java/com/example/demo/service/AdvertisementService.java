package com.example.demo.service;

import com.example.demo.model.AdPlacement;
import com.example.demo.model.AdStatus;
import com.example.demo.model.Advertisement;
import com.example.demo.repository.AdvertisementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AdvertisementService {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    public Advertisement saveAdvertisement(Advertisement ad) {
        return advertisementRepository.save(ad);
    }

    public List<Advertisement> getValidAdsForPlacement(AdPlacement placement) {
        // Fetch ads that are ACTIVE or SCHEDULED (might have become active)
        List<Advertisement> candidates = advertisementRepository.findByPlacementAndStatusInOrderByPriorityDesc(
                placement, Arrays.asList(AdStatus.ACTIVE, AdStatus.SCHEDULED));
                
        List<Advertisement> validAds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        boolean needsSave = false;

        for (Advertisement ad : candidates) {
            boolean changed = updateStatusIntelligently(ad, now);
            if (changed) {
                needsSave = true;
            }
            if (ad.getStatus() == AdStatus.ACTIVE) {
                // Check limit if applicable
                if (ad.getMaxImpressions() != null && ad.getImpressionCount() >= ad.getMaxImpressions()) {
                    ad.setStatus(AdStatus.PAUSED); // Or EXPIRED
                    needsSave = true;
                } else if (ad.getMaxClicks() != null && ad.getClickCount() >= ad.getMaxClicks()) {
                    ad.setStatus(AdStatus.PAUSED);
                    needsSave = true;
                } else {
                    validAds.add(ad);
                }
            }
        }

        if (needsSave) {
            advertisementRepository.saveAll(candidates);
        }

        return validAds;
    }
    
    // Updates status if time has passed start/end dates. Returns true if status changed.
    public boolean updateStatusIntelligently(Advertisement ad, LocalDateTime now) {
        if (ad.getStatus() == AdStatus.PAUSED || ad.getStatus() == AdStatus.DRAFT) {
            return false; // Leave paused/draft ads alone
        }
        
        AdStatus originalStatus = ad.getStatus();
        
        if (ad.getEndDateTime() != null && now.isAfter(ad.getEndDateTime())) {
            ad.setStatus(AdStatus.EXPIRED);
        } else if (ad.getStartDateTime() != null) {
            if (now.isBefore(ad.getStartDateTime())) {
                ad.setStatus(AdStatus.SCHEDULED);
            } else {
                ad.setStatus(AdStatus.ACTIVE);
            }
        }
        
        return originalStatus != ad.getStatus();
    }

    @Transactional
    public void incrementImpression(Long id) {
        advertisementRepository.findById(id).ifPresent(ad -> {
            ad.setImpressionCount(ad.getImpressionCount() + 1);
            advertisementRepository.save(ad);
        });
    }

    @Transactional
    public String registerClickAndGetUrl(Long id) {
        return advertisementRepository.findById(id).map(ad -> {
            ad.setClickCount(ad.getClickCount() + 1);
            advertisementRepository.save(ad);
            return ad.getCtaUrl();
        }).orElse("/");
    }
}
