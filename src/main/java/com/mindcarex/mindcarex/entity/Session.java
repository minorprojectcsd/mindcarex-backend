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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false)
    private String status; // IN_PROGRESS, ENDED

    @Column(nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    private OffsetDateTime endedAt;

    private Integer durationMinutes;  // Calculated duration

    @PrePersist
    protected void onCreate() {
        startedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (endedAt != null && startedAt != null) {
            long minutes = java.time.Duration.between(startedAt, endedAt).toMinutes();
            this.durationMinutes = (int) minutes;
        }
    }

    @Column(columnDefinition = "TEXT")
    private String summary;
}
