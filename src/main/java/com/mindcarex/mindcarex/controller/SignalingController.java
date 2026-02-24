package com.mindcarex.mindcarex.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
public class SignalingController {

    @MessageMapping("/signal/{sessionId}")
    @SendTo("/topic/signal/{sessionId}")
    public Map<String, Object> handleSignal(
            @DestinationVariable UUID sessionId,
            @Payload Map<String, Object> signal,
            SimpMessageHeaderAccessor headerAccessor
    ) {

        // Get WebSocket session ID
        String wsSessionId = headerAccessor.getSessionId();

        // Attach WebSocket session ID for frontend filtering
        signal.put("wsSessionId", wsSessionId);

        // Debug log
        System.out.println(
                "[WebRTC Signaling] Type: " + signal.get("type") +
                " | From: " + signal.get("from") +
                " | WS Session: " + wsSessionId
        );

        return signal;
    }
}