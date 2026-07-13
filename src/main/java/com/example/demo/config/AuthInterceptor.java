package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.example.demo.config.JwtUtil;
import com.example.demo.config.TokenBlacklist;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }
        
        // Skip public paths and game/socket endpoints
        if (path.equals("/") || path.equals("/home") || path.equals("/login") || 
            path.equals("/register") || path.equals("/about") || path.equals("/about-us") || path.equals("/careers") || path.equals("/privacy") || path.equals("/privacy-policy") || path.equals("/terms") || path.equals("/terms-of-service") || path.equals("/faq") || path.equals("/featured-events") || path.equals("/categories") || path.equals("/support") || path.equals("/contact") || path.equals("/debug-users") ||
            // Allow unauthenticated multiplayer room creation/join
            path.startsWith("/api/ludo/") ||
            path.startsWith("/api/snake/") ||
            path.startsWith("/api/uno/") ||
            path.startsWith("/api/chess/") ||
            path.startsWith("/api/rps/") ||
            path.startsWith("/ws") ||
            path.startsWith("/css/") || 
            path.startsWith("/js/") || path.startsWith("/images/") || path.startsWith("/uploads/")) {
            return true;
        }

        String token = null;

        // 1. Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2. Check Cookie (for traditional links/SSR)
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 3. Check Query Parameter (for multi-tab support)
        String queryToken = request.getParameter("auth");
        if (queryToken != null) {
            token = queryToken;
        }

        StringBuilder debugLog = new StringBuilder();
        debugLog.append("AuthInterceptor Debug Log\n");
        if (token != null) {
            debugLog.append("token found = ").append(token).append("\n");
            // ── Reject blacklisted (logged-out) tokens immediately ──
            if (tokenBlacklist.isBlacklisted(token)) {
                debugLog.append("token is blacklisted!\n");
                writeDebug(debugLog.toString());
                if (isAjaxRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login?expired=true");
                }
                return false;
            }

            request.setAttribute("urlToken", token); // Store for postHandle
            try {
                String username = jwtUtil.extractUsername(token);
                debugLog.append("extracted username = ").append(username).append("\n");
                if (username != null) {
                    if ("admin".equals(username)) {
                        request.setAttribute("authenticatedUser", "admin");
                        // Prevent browser from caching protected pages
                        setNoCacheHeaders(response);
                        return true;
                    }
                    User user = userRepository.findByUsername(username);
                    debugLog.append("user found = ").append(user != null).append("\n");
                    if (user != null && jwtUtil.validateToken(token, username)) {
                        // Store user in request for controllers to use
                        request.setAttribute("authenticatedUser", user);
                        // Prevent browser from caching protected pages
                        setNoCacheHeaders(response);
                        return true;
                    } else {
                        debugLog.append("validateToken failed or user is null!\n");
                    }
                }
            } catch (Exception e) {
                debugLog.append("Exception caught: ").append(e.getMessage()).append("\n");
                // Token invalid or expired, clear it
                jakarta.servlet.http.Cookie badCookie = new jakarta.servlet.http.Cookie("jwtToken", null);
                badCookie.setPath("/");
                badCookie.setHttpOnly(true);
                badCookie.setMaxAge(0);
                response.addCookie(badCookie);
            }
        } else {
            debugLog.append("token is NULL!\n");
        }

        // Not authenticated
        debugLog.append("redirecting to /login?error=timeout\n");
        writeDebug(debugLog.toString());
        if (isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        response.sendRedirect(request.getContextPath() + "/login?error=timeout");
        return false;
    }

    private void writeDebug(String msg) {
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("auth_debug.txt"), msg.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch(Exception e) {}
    }

    /**
     * Determines if a request is an AJAX or API request.
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith) || 
               path.startsWith("/api/") || 
               path.endsWith("/ajax");
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, org.springframework.web.servlet.ModelAndView modelAndView) throws Exception {
        // Removed to prevent appending ?auth=token to all redirect URLs. 
        // Authentication works properly via the HTTP-Only cookie.
    }

    /** Set HTTP headers that prevent the browser from caching protected pages.
     *  After logout, hitting Back-button will trigger a fresh request
     *  (which the blacklist check will immediately reject). */
    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}

