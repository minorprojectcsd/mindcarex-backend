package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionRepository sessionRepo;
    private final AppointmentRepository appointmentRepo;
    private final UserRepository userRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final ChatMessageRepository chatMessageRepo;

    // ✅ START SESSION (Doctor only) - ⭐ UPDATED
    @PostMapping("/{appointmentId}/start")
    public ResponseEntity<?> startSession(
            @PathVariable UUID appointmentId,
            Authentication auth
    ) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();

        if (!"DOCTOR".equals(user.getRole())) {
            return ResponseEntity.status(403).body("Only doctor can start session");
        }

        Doctor doctor = doctorRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Doctor profile missing"));

        Appointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            return ResponseEntity.status(403).body("Not your appointment");
        }

        // ⭐ NEW: Update appointment status to IN_PROGRESS
        appointment.setStatus("IN_PROGRESS");
        appointmentRepo.save(appointment);

        Session session = sessionRepo.findByAppointment(appointment)
                .orElse(
                        Session.builder()
                                .appointment(appointment)
                                .status("IN_PROGRESS")
                                .startedAt(OffsetDateTime.now())
                                .build()
                );

        sessionRepo.save(session);

        return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "status", session.getStatus()
        ));
    }

    // ✅ GET SESSION (Doctor or Patient)
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(
            @PathVariable UUID sessionId,
            Authentication auth
    ) {
        Session session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Validate user is participant
        if (!isSessionParticipant(session, auth)) {
            return ResponseEntity.status(403).body("Not authorized to view this session");
        }

        return ResponseEntity.ok(session);
    }

    // ⭐ NEW: GET CHAT HISTORY
    @GetMapping("/{sessionId}/chat")
    public ResponseEntity<?> getChatHistory(
            @PathVariable UUID sessionId,
            Authentication auth
    ) {
        Session session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Validate user is participant
        if (!isSessionParticipant(session, auth)) {
            return ResponseEntity.status(403).body("Not authorized to view chat");
        }

        var messages = chatMessageRepo.findBySessionIdOrderByTimestampAsc(sessionId);

        return ResponseEntity.ok(messages);
    }

    // ✅ END SESSION (Doctor only) - ⭐ UPDATED
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> endSession(
            @PathVariable UUID sessionId,
            Authentication auth
    ) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();

        if (!"DOCTOR".equals(user.getRole())) {
            return ResponseEntity.status(403).body("Only doctor can end session");
        }

        Session session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // ⭐ NEW: Update appointment status to COMPLETED
        Appointment appointment = session.getAppointment();
        appointment.setStatus("COMPLETED");
        appointmentRepo.save(appointment);

        session.setStatus("ENDED");
        session.setEndedAt(OffsetDateTime.now());
        sessionRepo.save(session);

        return ResponseEntity.ok("Session ended");
    }

    // Helper method to validate session participant
    private boolean isSessionParticipant(Session session, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();

        // Get doctor and patient from appointment
        Doctor doctor = session.getAppointment().getDoctor();
        Patient patient = session.getAppointment().getPatient();

        UUID doctorUserId = doctor.getUserId();
        UUID patientUserId = patient.getUserId();

        return user.getId().equals(doctorUserId) || user.getId().equals(patientUserId);
    }
}