
package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.service.*;
import com.mindcarex.mindcarex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

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
    private final EmailService emailService;

    @Value("${REPORT_API_URL:https://mindcarex-report-api.onrender.com}")
    private String reportApiUrl;

    // ─────────────────────────────────────────────
    // START SESSION (Doctor only)
    // ─────────────────────────────────────────────
    @PostMapping("/{appointmentId}/start")
    public ResponseEntity<?> startSession(
            @PathVariable UUID appointmentId,
            Authentication auth
    ) {
        try {
            if (auth == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

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

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // GET SESSION (Doctor / Patient)
    // ─────────────────────────────────────────────
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(
            @PathVariable UUID sessionId,
            Authentication auth
    ) {
        try {
            if (auth == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Session session = sessionRepo.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            Appointment appointment = session.getAppointment();
            Doctor doctor = appointment.getDoctor();
            Patient patient = appointment.getPatient();

            UUID doctorUserId = doctor.getUser().getId();
            UUID patientUserId = patient.getUser().getId();

            if (!user.getId().equals(doctorUserId) &&
                !user.getId().equals(patientUserId)) {
                return ResponseEntity.status(403).body("Not authorized");
            }

            return ResponseEntity.ok(session);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // END SESSION (Doctor only)
    // ─────────────────────────────────────────────
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> endSession(
            @PathVariable UUID sessionId,
            @RequestBody(required = false) Map<String, Object> summaryData,
            Authentication auth
    ) {
        try {
            if (auth == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"DOCTOR".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Only doctor can end session");
            }

            Session session = sessionRepo.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            Appointment appointment = session.getAppointment();
            appointment.setStatus("COMPLETED");
            appointmentRepo.save(appointment);

            session.setStatus("ENDED");
            session.setEndedAt(OffsetDateTime.now());
            sessionRepo.save(session);

            if (summaryData != null) {
                Doctor doctor = appointment.getDoctor();
                Patient patient = appointment.getPatient();

                User doctorUser = doctor.getUser();
                User patientUser = patient.getUser();

                String aiSummary = (String) summaryData.get("aiSummary");
                List<String> keyPoints = (List<String>) summaryData.get("keyPoints");
                String recommendations = (String) summaryData.get("recommendations");
                String nextSteps = (String) summaryData.get("nextSteps");

                emailService.sendSessionSummary(
                        session, doctorUser, aiSummary, keyPoints, recommendations, nextSteps
                );

                emailService.sendSessionSummary(
                        session, patientUser, aiSummary, keyPoints, recommendations, nextSteps
                );

                emailService.sendSessionSummaryToGuardian(
                        session,
                        patient,
                        aiSummary,
                        keyPoints,
                        recommendations,
                        fetchGuardianMessage(session.getId())
                );
            }

            return ResponseEntity.ok("Session ended");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Fetch AI Guardian Message
    // ─────────────────────────────────────────────
    private String fetchGuardianMessage(UUID sessionId) {
        try {
            RestTemplate rest = new RestTemplate();
            String url = reportApiUrl + "/api/report/" + sessionId;

            Map<String, Object> response = rest.getForObject(url, Map.class);
            if (response == null) return null;

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) return null;

            return (String) data.get("guardian_message");

        } catch (Exception e) {
            return null; // fail silently
        }
    }
}
