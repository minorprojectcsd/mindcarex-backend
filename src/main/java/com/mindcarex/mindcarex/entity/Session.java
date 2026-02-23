package com.mindcarex.mindcarex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;

    @Column(nullable = false)
    private String status; // SCHEDULED, IN_PROGRESS, ENDED

    @Column(columnDefinition = "TEXT")
    private String summary;
}
