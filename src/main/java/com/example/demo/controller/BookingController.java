package com.example.demo.controller;

import com.example.demo.model.EventRegistration;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/user/my-bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    private User getUserFromSession(HttpSession session) {
        if (session != null) {
            try {
                User user = (User) session.getAttribute("user");
                if (user != null) return user;
                Long userId = (Long) session.getAttribute("userId");
                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception e) {}
        }
        return null;
    }

    @GetMapping
    public String myBookings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "All") String filter,
            @RequestParam(required = false, defaultValue = "Newest First") String sort,
            Model model, HttpSession session) {
        
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);

        List<EventRegistration> bookings = bookingService.getFilteredAndSortedBookings(user, search, filter, sort);

        model.addAttribute("bookings", bookings);
        model.addAttribute("activeSearch", search != null ? search : "");
        model.addAttribute("activeFilter", filter);
        model.addAttribute("activeSort", sort);
        model.addAttribute("user", user);

        return "user/my-bookings";
    }

    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }

        boolean success = bookingService.cancelBooking(id, user);
        if (success) {
            return "redirect:/user/my-bookings?cancelSuccess=true";
        } else {
            return "redirect:/user/my-bookings?error=true";
        }
    }
}
