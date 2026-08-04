package com.example.demo.repository;

import com.example.demo.model.Event;
import com.example.demo.model.SecretRewardPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecretRewardPartnerRepository extends JpaRepository<SecretRewardPartner, Long> {
    List<SecretRewardPartner> findByEvent(Event event);
}
