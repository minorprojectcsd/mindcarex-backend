package com.mindcarex.mindcarex.repository;

import com.mindcarex.mindcarex.entity.Session;
import com.mindcarex.mindcarex.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByAppointment(Appointment appointment);
}
