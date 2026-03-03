package com.mindcarex.mindcarex.repository;

import com.mindcarex.mindcarex.entity.Appointment;
import com.mindcarex.mindcarex.entity.Doctor;
import com.mindcarex.mindcarex.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByDoctor(Doctor doctor);

    List<Appointment> findByPatient(Patient patient);

    // ⭐ For 10-minute reminder scheduler
    List<Appointment> findByScheduledAtBetweenAndStatus(
            OffsetDateTime start,
            OffsetDateTime end,
            String status
    );
}