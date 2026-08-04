package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import jakarta.servlet.http.Cookie;
import java.security.Principal;
import java.util.Map;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor(), new JwtHandshakeInterceptor())
                .setHandshakeHandler(new UserHandshakeHandler())
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor(), new JwtHandshakeInterceptor())
                .setHandshakeHandler(new UserHandshakeHandler())
                .withSockJS();
    }

    /**
     * The app's primary auth is a JWT cookie (validated per-request by AuthInterceptor),
     * not the HttpSession. Session attributes can be empty/stale (e.g. after a server
     * restart) even while the JWT cookie is still valid, which used to leave the WebSocket
     * Principal null and crash every @MessageMapping handler. Resolve the user from the
     * same JWT cookie here so WebSocket auth matches the rest of the app.
     */
    private class JwtHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (attributes.get("user") != null) return true; // already resolved via session
            if (!(request instanceof ServletServerHttpRequest servletRequest)) return true;

            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
            if (cookies == null) return true;

            for (Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName())) {
                    try {
                        String username = jwtUtil.extractUsername(cookie.getValue());
                        if (username != null && jwtUtil.validateToken(cookie.getValue(), username)) {
                            User user = userRepository.findByUsername(username);
                            if (user != null) {
                                attributes.put("user", user);
                            }
                        }
                    } catch (Exception ignored) {
                        // Invalid/expired token: leave unauthenticated, handled below
                    }
                    break;
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {
        }
    }

    private class UserHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                Map<String, Object> attributes) {
            User user = (User) attributes.get("user");
            if (user != null) {
                final String userId = String.valueOf(user.getId());
                return new Principal() {
                    @Override
                    public String getName() {
                        return userId;
                    }
                };
            }
            return null;
        }
    }
}
