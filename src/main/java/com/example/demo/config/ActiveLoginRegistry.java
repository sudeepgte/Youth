package com.example.demo.config;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveLoginRegistry {

    public static class ActiveSession {
        private final String token;
        private long lastActivity;

        public ActiveSession(String token) {
            this.token = token;
            this.lastActivity = System.currentTimeMillis();
        }

        public String getToken() {
            return token;
        }

        public long getLastActivity() {
            return lastActivity;
        }

        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
    }

    private final Map<String, ActiveSession> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes inactivity timeout

    public synchronized boolean isUserAlreadyLoggedIn(String username, String currentToken) {
        ActiveSession session = activeSessions.get(username);
        if (session == null) {
            return false;
        }
        // If the active token is the same as the current request's token, it's the same session/device
        if (session.getToken().equals(currentToken)) {
            return false;
        }
        // Check if the session has timed out due to inactivity
        if (System.currentTimeMillis() - session.getLastActivity() > SESSION_TIMEOUT_MS) {
            // Remove the timed out session
            activeSessions.remove(username);
            return false;
        }
        return true;
    }

    public void registerLogin(String username, String token) {
        activeSessions.put(username, new ActiveSession(token));
    }

    public void updateActivity(String username, String token) {
        ActiveSession session = activeSessions.get(username);
        if (session != null && session.getToken().equals(token)) {
            session.updateActivity();
        }
    }

    public void removeLogin(String username) {
        if (username != null) {
            activeSessions.remove(username);
        }
    }
}
