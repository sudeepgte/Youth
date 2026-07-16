package com.example.demo.repository;

import com.example.demo.model.EventRegistration;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    List<EventRegistration> findByEventAndUser(Event event, User user);
    Optional<EventRegistration> findByTicketId(String ticketId);
    List<EventRegistration> findByUser(User user);
    List<EventRegistration> findByUserOrderByRegistrationDateDesc(User user);
    List<EventRegistration> findByEvent(Event event);
    long countByEvent(Event event);
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(r.quantity), 0L) FROM EventRegistration r WHERE r.event = :event")
    long sumQuantityByEvent(@org.springframework.data.repository.query.Param("event") Event event);
    long countByEventAndAttendanceMarked(Event event, boolean marked);
    long countByUser(User user);
    long countByUserAndPosition(User user, String position);
    long countByUserAndAttendanceMarked(User user, boolean marked);

    @org.springframework.data.jpa.repository.Query("SELECT r.event.id, COUNT(r) FROM EventRegistration r WHERE r.event.id IN :eventIds AND r.registrationStatus = 'REGISTERED' GROUP BY r.event.id")
    List<Object[]> countActiveBookingsForEvents(@org.springframework.data.repository.query.Param("eventIds") List<Long> eventIds);
}
