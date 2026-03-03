package com.mindcarex.mindcarex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // 🔥 Correct OneToOne mapping
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String fullName;

    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String gender;

    private Integer age;

    @Column(length = 2000)
    private String address;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactEmail;
    private String emergencyContactRelation;

    @Column(length = 5000)
    private String medicalHistory;

    private String bloodGroup;
    private String allergies;

    @Column(length = 2000)
    private String currentMedications;

    private String insuranceProvider;
    private String insuranceNumber;

    // 🔥 Better: relationship instead of raw UUID
    @ManyToOne
    @JoinColumn(name = "primary_doctor_id")
    private Doctor primaryDoctor;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        calculateAge();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateAge();
    }

    private void calculateAge() {
        if (dateOfBirth != null) {
            this.age = LocalDate.now().getYear() - dateOfBirth.getYear();
        }
    }
}