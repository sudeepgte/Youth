package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LiveStreamController {

    @Autowired
    private UserRepository userRepository;

    private User getUserFromSession(HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            return (User) sessionUser;
        }
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = null;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }
                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    @GetMapping("/live")
    public String liveStreamPage(@RequestParam(required = false, defaultValue = "Live Stream") String title,
                                 @RequestParam(required = false, defaultValue = "false") boolean start,
                                 @RequestParam(required = false) String roomId,
                                 HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        
        String streamRoomId = roomId;
        if (streamRoomId == null || streamRoomId.isEmpty()) {
            if (start) {
                streamRoomId = user.getUsername() + "_" + System.currentTimeMillis();
            } else {
                return "redirect:/"; // Should not happen directly
            }
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("title", title);
        model.addAttribute("isBroadcaster", start);
        model.addAttribute("roomId", streamRoomId);
        return "live";
    }
}
