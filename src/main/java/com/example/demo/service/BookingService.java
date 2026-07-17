package com.example.demo.service;

import com.example.demo.model.EventRegistration;
import com.example.demo.model.EventSeat;
import com.example.demo.model.User;
import com.example.demo.repository.EventRegistrationRepository;
import com.example.demo.repository.EventSeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    public List<EventRegistration> getBookingsByUser(User user) {
        return eventRegistrationRepository.findByUserOrderByRegistrationDateDesc(user);
    }

    public List<EventRegistration> getFilteredAndSortedBookings(User user, String search, String filter, String sort) {
        List<EventRegistration> bookings = getBookingsByUser(user);
        
        // 1. Search Filter
        if (search != null && !search.trim().isEmpty()) {
            String term = search.toLowerCase().trim();
            bookings = bookings.stream().filter(b -> 
                (b.getEvent() != null && b.getEvent().getTitle() != null && b.getEvent().getTitle().toLowerCase().contains(term)) ||
                (b.getTicketId() != null && b.getTicketId().toLowerCase().contains(term))
            ).collect(Collectors.toList());
        }
        
        // 2. Status Filter
        LocalDateTime now = LocalDateTime.now();
        if (filter != null && !filter.equalsIgnoreCase("All")) {
            bookings = bookings.stream().filter(b -> {
                if ("Cancelled".equalsIgnoreCase(filter)) {
                    return "CANCELLED".equalsIgnoreCase(b.getRegistrationStatus());
                } else if ("Completed".equalsIgnoreCase(filter)) {
                    return !"CANCELLED".equalsIgnoreCase(b.getRegistrationStatus()) && 
                           b.getEvent() != null && b.getEvent().getDateTime() != null && 
                           b.getEvent().getDateTime().isBefore(now);
                } else if ("Upcoming".equalsIgnoreCase(filter)) {
                    return !"CANCELLED".equalsIgnoreCase(b.getRegistrationStatus()) && 
                           b.getEvent() != null && b.getEvent().getDateTime() != null && 
                           b.getEvent().getDateTime().isAfter(now);
                }
                return true;
            }).collect(Collectors.toList());
        }
        
        // 3. Sorting
        if ("Oldest First".equalsIgnoreCase(sort)) {
            bookings.sort((b1, b2) -> {
                if (b1.getRegistrationDate() == null) return 1;
                if (b2.getRegistrationDate() == null) return -1;
                return b1.getRegistrationDate().compareTo(b2.getRegistrationDate());
            });
        }
        // "Newest First" is the default from the repository
        
        return bookings;
    }

    public boolean cancelBooking(Long regId, User user) {
        EventRegistration reg = eventRegistrationRepository.findById(regId).orElse(null);
        if (reg == null || reg.getUser() == null || !reg.getUser().getId().equals(user.getId())) {
            return false;
        }
        
        // Can only cancel if event is in the future
        LocalDateTime now = LocalDateTime.now();
        if (reg.getEvent() != null && reg.getEvent().getDateTime() != null && reg.getEvent().getDateTime().isBefore(now)) {
            return false;
        }
        
        if ("CANCELLED".equalsIgnoreCase(reg.getRegistrationStatus())) {
            return false;
        }

        // 1. Mark as cancelled
        reg.setRegistrationStatus("CANCELLED");
        eventRegistrationRepository.save(reg);

        // 2. Release seats (if any were booked)
        List<EventSeat> bookedSeats = eventSeatRepository.findByEventAndBookedByUser(reg.getEvent(), user);
        if (bookedSeats != null && !bookedSeats.isEmpty()) {
            for (EventSeat seat : bookedSeats) {
                seat.setStatus("AVAILABLE");
                seat.setBookedByUser(null);
            }
            eventSeatRepository.saveAll(bookedSeats);
        }
        
        // Decrement registered count in tier if applicable
        if (reg.getSelectedTier() != null && reg.getEvent() != null && reg.getEvent().getSeatTiers() != null) {
            for (com.example.demo.model.EventSeatTier t : reg.getEvent().getSeatTiers()) {
                if (reg.getSelectedTier().equalsIgnoreCase(t.getTierName())) {
                    if (t.getRegisteredCount() != null && t.getRegisteredCount() >= reg.getQuantity()) {
                        t.setRegisteredCount(t.getRegisteredCount() - reg.getQuantity());
                    }
                    break;
                }
            }
        }

        return true;
    }
}
