package com.example.demo.controller;

import com.example.demo.config.ActiveLoginRegistry;
import com.example.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    @Autowired
    private ActiveLoginRegistry activeLoginRegistry;

    @RequestMapping(value = "/keepalive", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> keepAlive(HttpServletRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Object authUser = request.getAttribute("authenticatedUser");

        String username = null;
        if (authUser instanceof User) {
            username = ((User) authUser).getUsername();
        } else if (authUser instanceof String) {
            username = (String) authUser;
        } else if (session.getAttribute("user") instanceof User) {
            username = ((User) session.getAttribute("user")).getUsername();
        } else if (session.getAttribute("user") instanceof String) {
            username = (String) session.getAttribute("user");
        }

        if (username != null) {
            String token = (String) request.getAttribute("urlToken");
            activeLoginRegistry.updateActivity(username, token);
            session.setAttribute("lastActivityTime", System.currentTimeMillis());

            response.put("status", "ok");
            response.put("authenticated", true);
            response.put("username", username);
            response.put("timeoutSeconds", 28800); // 8 hours
            response.put("warningSeconds", 300);   // 5 minutes
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        }

        response.put("status", "error");
        response.put("authenticated", false);
        response.put("message", "Session expired or invalid.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(HttpServletRequest request, HttpSession session) {
        return keepAlive(request, session);
    }
}
