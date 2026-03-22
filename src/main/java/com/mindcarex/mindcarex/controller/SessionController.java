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
import java.util.HashMap;

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

    /**
     * POST /api/sessions/{appointmentId}/start
     * Doctor starts a session for an appointment
     */
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

    /**
     * GET /api/sessions/{sessionId}
     * Returns session details
     * ACCESSIBLE BY: Doctor OR Patient (if they're part of this session)
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(
            @PathVariable UUID sessionId,
            Authentication auth
    ) {
        try {
            System.out.println("=== SESSION DEBUG START ===");
            System.out.println("Session ID requested: " + sessionId);
            System.out.println("User email from auth: " + auth.getName());
            
            // Get user
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            System.out.println("User found - ID: " + user.getId() + ", Role: " + user.getRole());
            
            // Get session
            Session session = sessionRepo.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            System.out.println("Session found - ID: " + session.getId());
            
            // Get appointment
            Appointment appointment = session.getAppointment();
            System.out.println("Appointment ID: " + appointment.getId());
            
            // Get doctor and patient
            Doctor doctor = appointment.getDoctor();
            Patient patient = appointment.getPatient();
            
            System.out.println("Doctor profile ID: " + doctor.getId());
            System.out.println("Patient profile ID: " + patient.getId());
            
            // Get user IDs - with null checks
            UUID doctorUserId = null;
            UUID patientUserId = null;
            
            try {
                doctorUserId = doctor.getUser().getId();
                System.out.println("Doctor USER ID: " + doctorUserId);
            } catch (NullPointerException e) {
                System.out.println("⚠️ Doctor user is NULL!");
            }
            
            try {
                patientUserId = patient.getUser().getId();
                System.out.println("Patient USER ID: " + patientUserId);
            } catch (NullPointerException e) {
                System.out.println("⚠️ Patient user is NULL!");
            }
            
            System.out.println("Current user ID: " + user.getId());
            
            boolean isDoctorMatch = doctorUserId != null && user.getId().equals(doctorUserId);
            boolean isPatientMatch = patientUserId != null && user.getId().equals(patientUserId);
            
            System.out.println("Is doctor? " + isDoctorMatch);
            System.out.println("Is patient? " + isPatientMatch);
            
            if (!isDoctorMatch && !isPatientMatch) {
                System.out.println("❌ ACCESS DENIED - User is neither doctor nor patient");
                System.out.println("=== SESSION DEBUG END ===");
                
                Map<String, Object> debugInfo = new HashMap<>();
                debugInfo.put("userId", user.getId().toString());
                if (doctorUserId != null) debugInfo.put("doctorUserId", doctorUserId.toString());
                if (patientUserId != null) debugInfo.put("patientUserId", patientUserId.toString());
                
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Not authorized",
                    "debug", debugInfo
                ));
            }
            
            System.out.println("✅ ACCESS GRANTED");
            System.out.println("=== SESSION DEBUG END ===");
            
            return ResponseEntity.ok(session);
            
        } catch (Exception e) {
            System.out.println("❌ EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=== SESSION DEBUG END ===");
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/sessions/{sessionId}/chat
     * Returns chat history for session
     */
    @GetMapping("/{sessionId}/chat")
    public ResponseEntity<?> getChatHistory(
            @PathVariable UUID sessionId,
            Authentication auth
    ) {
        Session session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!isSessionParticipant(session, auth)) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        var messages = chatMessageRepo.findBySessionIdOrderByTimestampAsc(sessionId);

        return ResponseEntity.ok(messages);
    }

    /**
     * POST /api/sessions/{sessionId}/end
     * Doctor ends the session
     */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> endSession(
            @PathVariable UUID sessionId,
            @RequestBody(required = false) Map<String, Object> summaryData,
            Authentication auth
    ) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();

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
            @SuppressWarnings("unchecked")
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
                    session, patient, aiSummary, keyPoints, recommendations,
                    fetchGuardianMessage(session.getId())
            );
        }

        return ResponseEntity.ok("Session ended");
    }

    /**
     * Fetch AI guardian message from report service
     * Returns null gracefully if service is unavailable
     */
    private String fetchGuardianMessage(UUID sessionId) {
        try {
            RestTemplate rest = new RestTemplate();
            String url = reportApiUrl + "/api/report/" + sessionId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = rest.getForObject(url, Map.class);
            if (response == null) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) return null;
            return (String) data.get("guardian_message");
        } catch (Exception e) {
            // Service not available or report not generated yet
            return null;
        }
    }

    /**
     * Check if authenticated user is a participant in this session
     * (either the doctor or the patient)
     */
    private boolean isSessionParticipant(Session session, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();

        Doctor doctor = session.getAppointment().getDoctor();
        Patient patient = session.getAppointment().getPatient();

        // Get the USER ID linked to doctor and patient profiles
        UUID doctorUserId = doctor.getUser().getId();
        UUID patientUserId = patient.getUser().getId();

        return user.getId().equals(doctorUserId) || user.getId().equals(patientUserId);
    }
}
