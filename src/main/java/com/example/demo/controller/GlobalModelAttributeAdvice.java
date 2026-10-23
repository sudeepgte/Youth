package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.ChatService;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalModelAttributeAdvice {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @ModelAttribute
    public void addGlobalAttributes(HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user != null) {
            // Always refresh from DB to get latest coins/xp
            user = userRepository.findById(user.getId()).orElse(user);
            
            long unreadCount = chatService.getUnreadCount(user);
            model.addAttribute("unreadMessageCount", unreadCount);
            model.addAttribute("user", user); // Ensure "user" is the one used in templates
            model.addAttribute("currentUser", user); 
        }

        Object token = httpServletRequest.getAttribute("urlToken");
        if (token instanceof String && !((String) token).isBlank()) {
            model.addAttribute("auth", token);
        }

        if (httpServletRequest != null) {
            try {
                String scheme = httpServletRequest.getScheme();
                String serverName = httpServletRequest.getServerName();
                int serverPort = httpServletRequest.getServerPort();
                String contextPath = httpServletRequest.getContextPath();
                String portPart = "";
                if (("http".equalsIgnoreCase(scheme) && serverPort != 80) ||
                    ("https".equalsIgnoreCase(scheme) && serverPort != 443)) {
                    portPart = ":" + serverPort;
                }
                String baseUrl = scheme + "://" + serverName + portPart + (contextPath != null ? contextPath : "");
                model.addAttribute("baseUrl", baseUrl);
                model.addAttribute("serverPort", serverPort);
            } catch (Exception ignored) {
                model.addAttribute("baseUrl", "http://localhost:8009");
                model.addAttribute("serverPort", 8009);
            }
        } else {
            model.addAttribute("baseUrl", "http://localhost:8009");
            model.addAttribute("serverPort", 8009);
        }
    }

    private User getUserFromSession(HttpSession session) {
        // First check request attribute from JWT
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return (User) authUser;
        }

        if (session == null)
            return null;
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
                // Ignore recovery failure
            }
        }
        return null;
    }
}
