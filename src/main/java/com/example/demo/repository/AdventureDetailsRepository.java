package com.example.demo.repository;

import com.example.demo.model.AdventureDetails;
import com.example.demo.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdventureDetailsRepository extends JpaRepository<AdventureDetails, Long> {
    Optional<AdventureDetails> findByEvent(Event event);
}
