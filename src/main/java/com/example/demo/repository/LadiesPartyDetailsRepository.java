package com.example.demo.repository;

import com.example.demo.model.Event;
import com.example.demo.model.LadiesPartyDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LadiesPartyDetailsRepository extends JpaRepository<LadiesPartyDetails, Long> {
    Optional<LadiesPartyDetails> findByEvent(Event event);
}
