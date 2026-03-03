package com.mindcarex.mindcarex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String specialization;

    @Column(unique = true)
    private String licenseNumber;

    private Integer experienceYears;

    @Column(length = 1000)
    private String qualifications;

    @Column(length = 2000)
    private String bio;

    private String languages;
    private String clinicAddress;
    private String consultationFee;

    @Column(nullable = false)
    private String availabilityStatus = "AVAILABLE";

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

}