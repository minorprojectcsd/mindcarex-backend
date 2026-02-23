package com.mindcarex.mindcarex.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Get allowed origins from environment
        String allowedOrigins = System.getenv("ALLOWED_ORIGINS");

        String[] origins;
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            origins = allowedOrigins.split(",");
        } else {
            origins = new String[]{"http://localhost:5173"};
        }

        registry
                .addEndpoint("/ws")
                .setAllowedOrigins(origins); // ⭐ CHANGED: Dynamic origins
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}