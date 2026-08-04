package com.example.demo.repository;

import com.example.demo.model.Event;
import com.example.demo.model.TrekkingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrekkingDetailsRepository extends JpaRepository<TrekkingDetails, Long> {
    Optional<TrekkingDetails> findByEvent(Event event);
}
