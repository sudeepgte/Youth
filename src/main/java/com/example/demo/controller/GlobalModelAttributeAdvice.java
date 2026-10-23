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

    @org.springframework.beans.factory.annotation.Value("${promotional.banner.enabled:true}")
    private boolean promoBannerEnabled;

    @org.springframework.beans.factory.annotation.Value("${promotional.banner.text:Youthians Spring Fest 2026 — Registrations now open!}")
    private String promoBannerText;

    @org.springframework.beans.factory.annotation.Value("${promotional.banner.link:/events}")
    private String promoBannerLink;

    @org.springframework.beans.factory.annotation.Value("${youthians.support.email:support@youthians.com}")
    private String supportEmail;

    @org.springframework.beans.factory.annotation.Value("${youthians.social.instagram:https://www.instagram.com/youthians}")
    private String socialInstagram;

    @org.springframework.beans.factory.annotation.Value("${youthians.social.twitter:https://x.com/youthians}")
    private String socialTwitter;

    @org.springframework.beans.factory.annotation.Value("${youthians.social.facebook:https://www.facebook.com/youthians}")
    private String socialFacebook;

    @ModelAttribute
    public void addGlobalAttributes(HttpSession session, Model model) {
        // Dynamic baseUrl for QR codes, links, and callbacks
        try {
            String scheme = httpServletRequest != null ? httpServletRequest.getScheme() : "http";
            String serverName = httpServletRequest != null ? httpServletRequest.getServerName() : "localhost";
            int serverPort = httpServletRequest != null ? httpServletRequest.getServerPort() : 8009;
            String contextPath = httpServletRequest != null ? httpServletRequest.getContextPath() : "";
            String portPart = (("http".equalsIgnoreCase(scheme) && serverPort == 80) || 
                               ("https".equalsIgnoreCase(scheme) && serverPort == 443)) ? "" : (":" + serverPort);
            String baseUrl = scheme + "://" + serverName + portPart + (contextPath != null ? contextPath : "");

            model.addAttribute("baseUrl", baseUrl);
            model.addAttribute("serverPort", serverPort);
        } catch (Exception ignored) {
            model.addAttribute("baseUrl", "http://localhost:8009");
            model.addAttribute("serverPort", 8009);
        }

        model.addAttribute("promoBannerEnabled", promoBannerEnabled);
        model.addAttribute("promoBannerText", promoBannerText);
        model.addAttribute("promoBannerLink", promoBannerLink);
        model.addAttribute("supportEmail", supportEmail);
        model.addAttribute("socialInstagram", socialInstagram);
        model.addAttribute("socialTwitter", socialTwitter);
        model.addAttribute("socialFacebook", socialFacebook);

        User user = getUserFromSession(session);
        if (user != null) {
            // Always refresh from DB to get latest coins/xp
            user = userRepository.findById(user.getId()).orElse(user);
            
            long unreadCount = chatService.getUnreadCount(user);
            model.addAttribute("unreadMessageCount", unreadCount);
            model.addAttribute("user", user); // Ensure "user" is the one used in templates
            model.addAttribute("currentUser", user); 
        }

        Object token = httpServletRequest != null ? httpServletRequest.getAttribute("urlToken") : null;
        if (token instanceof String && !((String) token).isBlank()) {
            model.addAttribute("auth", token);
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
