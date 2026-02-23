package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;

    // =========================
    // BOOK APPOINTMENT (PATIENT)
    // =========================
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Map<String, String> req,
            Authentication auth
    ) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"PATIENT".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Only patients can book");
            }

            Patient patient = patientRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Patient profile missing"));

            // Validate required fields
            if (req.get("doctorId") == null || req.get("doctorId").isEmpty()) {
                return ResponseEntity.badRequest().body("doctorId is required");
            }

            if (req.get("startTime") == null || req.get("startTime").isEmpty()) {
                return ResponseEntity.badRequest().body("startTime is required");
            }

            if (req.get("endTime") == null || req.get("endTime").isEmpty()) {
                return ResponseEntity.badRequest().body("endTime is required");
            }

            // Parse with error handling
            UUID doctorId;
            try {
                doctorId = UUID.fromString(req.get("doctorId"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid doctorId format");
            }

            OffsetDateTime startTime;
            OffsetDateTime endTime;
            try {
                startTime = OffsetDateTime.parse(req.get("startTime"));
                endTime = OffsetDateTime.parse(req.get("endTime"));
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body("Invalid date format. Use ISO-8601: 2026-02-15T10:00:00Z");
            }

            Doctor doctor = doctorRepo.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            Appointment appt = Appointment.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status("BOOKED")
                    .build();

            appointmentRepo.save(appt);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Appointment booked successfully",
                            "appointmentId", appt.getId()
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    // =========================
    // PATIENT → MY APPOINTMENTS
    // =========================
    @GetMapping("/my")
    public ResponseEntity<?> myAppointments(Authentication auth) {

        User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"PATIENT".equals(user.getRole())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        Patient patient = patientRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile missing"));

        List<Map<String, Object>> response = new ArrayList<>();

        for (Appointment a : appointmentRepo.findByPatient(patient)) {

            Map<String, Object> appt = new HashMap<>();
            appt.put("id", a.getId());
            appt.put("startTime", a.getStartTime());
            appt.put("endTime", a.getEndTime());
            appt.put("status", a.getStatus());

            // Include session info if exists
            Optional<Session> session = sessionRepo.findByAppointment(a);
            if (session.isPresent()) {
                appt.put("sessionId", session.get().getId());
                appt.put("sessionStatus", session.get().getStatus());
            } else {
                appt.put("sessionId", null);
                appt.put("sessionStatus", null);
            }

            // DOCTOR DETAILS
            Map<String, Object> doctor = new HashMap<>();
            doctor.put("id", a.getDoctor().getId());
            doctor.put("name", a.getDoctor().getFullName());
            doctor.put("specialization", a.getDoctor().getSpecialization());

            appt.put("doctor", doctor);

            response.add(appt);
        }

        return ResponseEntity.ok(response);
    }

    // =========================
    // CANCEL APPOINTMENT (⭐ NEW)
    // =========================
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable UUID id,
            Authentication auth
    ) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Appointment appointment = appointmentRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            // Check if user is authorized
            boolean isPatient = "PATIENT".equals(user.getRole()) &&
                    appointment.getPatient().getUserId().equals(user.getId());

            boolean isDoctor = "DOCTOR".equals(user.getRole()) &&
                    appointment.getDoctor().getUserId().equals(user.getId());

            if (!isPatient && !isDoctor) {
                return ResponseEntity.status(403)
                        .body("You are not authorized to cancel this appointment");
            }

            // Check if appointment can be cancelled
            if ("COMPLETED".equals(appointment.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Cannot cancel a completed appointment");
            }

            if ("CANCELLED".equals(appointment.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Appointment is already cancelled");
            }

            // If session is in progress, end it first
            Optional<Session> existingSession = sessionRepo.findByAppointment(appointment);
            if (existingSession.isPresent() &&
                    "IN_PROGRESS".equals(existingSession.get().getStatus())) {

                Session session = existingSession.get();
                session.setStatus("ENDED");
                session.setEndedAt(OffsetDateTime.now());
                sessionRepo.save(session);
            }

            // Cancel the appointment
            appointment.setStatus("CANCELLED");
            appointmentRepo.save(appointment);

            return ResponseEntity.ok(Map.of(
                    "message", "Appointment cancelled successfully",
                    "appointmentId", appointment.getId()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Internal server error: " + e.getMessage());
        }
    }
}