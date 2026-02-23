package com.mindcarex.mindcarex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

    @Entity
    @Table(name = "appointments")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class Appointment {

        @Id
        @GeneratedValue
        private UUID id;

        @ManyToOne
        @JoinColumn(name = "doctor_id", nullable = false)
        private Doctor doctor;

        @ManyToOne
        @JoinColumn(name = "patient_id", nullable = false)
        private Patient patient;

        @Column(nullable = false)
        private OffsetDateTime startTime;

        @Column(nullable = false)
        private OffsetDateTime endTime;

        @Column(nullable = false)
        private String status; // BOOKED, COMPLETED, CANCELLED

    }

