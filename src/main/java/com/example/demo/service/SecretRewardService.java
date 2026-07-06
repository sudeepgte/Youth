package com.example.demo.service;

import com.example.demo.model.Event;
import com.example.demo.model.EventRegistration;
import com.example.demo.model.SecretRewardPartner;
import com.example.demo.model.UserReward;
import com.example.demo.repository.SecretRewardPartnerRepository;
import com.example.demo.repository.UserRewardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SecretRewardService {

    @Autowired
    private SecretRewardPartnerRepository partnerRepository;

    @Autowired
    private UserRewardRepository userRewardRepository;

    private Random random = new Random();

    @Transactional
    public void assignReward(EventRegistration reg) {
        if (reg == null || reg.getEvent() == null || !reg.getEvent().isEnableSecretRewards()) {
            return;
        }

        // Check if user already got a reward for this event
        Optional<UserReward> existingReward = userRewardRepository.findByUserAndEvent(reg.getUser(), reg.getEvent());
        if (existingReward.isPresent()) {
            return; // Already has a reward
        }

        List<SecretRewardPartner> availablePartners = partnerRepository.findByEvent(reg.getEvent())
                .stream()
                .filter(p -> p.getRemainingQuantity() > 0)
                .collect(Collectors.toList());

        if (availablePartners.isEmpty()) {
            return; // No rewards left
        }

        SecretRewardPartner selectedPartner = availablePartners.get(random.nextInt(availablePartners.size()));
        
        // Decrement quantity
        selectedPartner.setRemainingQuantity(selectedPartner.getRemainingQuantity() - 1);
        partnerRepository.save(selectedPartner);

        // Assign reward
        UserReward userReward = new UserReward();
        userReward.setUser(reg.getUser());
        userReward.setEvent(reg.getEvent());
        userReward.setSecretReward(selectedPartner);
        userReward.setIssueDate(LocalDateTime.now());
        userReward.setExpiryDate(LocalDateTime.now().plusDays(selectedPartner.getValidityDays()));
        userReward.setStatus("AVAILABLE");
        userReward.setRewardCode(generateUniqueRewardCode());

        userRewardRepository.save(userReward);
    }

    private String generateUniqueRewardCode() {
        return "RWD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
