package com.mindcarex.mindcarex.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class SignalingController {

    @MessageMapping("/signal/{sessionId}")
    @SendTo("/topic/signal/{sessionId}")
    public SignalingMessage relay(
            @DestinationVariable String sessionId,
            SignalingMessage message
    ) {
        // Simply relay WebRTC signaling messages (OFFER, ANSWER, ICE)
        message.setSessionId(UUID.fromString(sessionId));
        return message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalingMessage {
        private String type; // "OFFER", "ANSWER", "ICE"
        private UUID sessionId;
        private Object payload; // SDP or ICE candidate data
    }
}