package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.ChatMessage;
import com.mindcarex.mindcarex.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat/{sessionId}")
    @SendTo("/topic/chat/{sessionId}")
    public ChatMessage sendMessage(
            @DestinationVariable String sessionId,
            ChatMessage message
    ) {
        // Set server-side values
        message.setSessionId(UUID.fromString(sessionId));
        message.setTimestamp(Instant.now());

        // Save to MongoDB (auto-deletes after 24 hours)
        ChatMessage saved = chatMessageRepository.save(message);

        // Broadcast to all subscribers of this session
        return saved;
    }
}