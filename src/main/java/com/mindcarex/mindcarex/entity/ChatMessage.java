package com.mindcarex.mindcarex.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "session_chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    private UUID sessionId;
    private UUID senderId;
    private String senderRole; // "DOCTOR" or "PATIENT"
    private String message;

    @Indexed(expireAfterSeconds = 86400) // Auto-delete after 24 hours
    private Instant timestamp;
}