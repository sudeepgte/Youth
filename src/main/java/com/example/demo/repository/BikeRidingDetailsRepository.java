package com.example.demo.repository;

import com.example.demo.model.Event;
import com.example.demo.model.BikeRidingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BikeRidingDetailsRepository extends JpaRepository<BikeRidingDetails, Long> {
    Optional<BikeRidingDetails> findByEvent(Event event);
}
