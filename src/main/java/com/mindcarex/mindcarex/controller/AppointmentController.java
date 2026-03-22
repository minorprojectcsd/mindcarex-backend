package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.*;
import com.mindcarex.mindcarex.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentRepository appointmentRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    private final EmailService emailService;

    /**
     * POST /api/appointments
     * Patient books appointment - Status: PENDING (waiting for doctor confirmation)
     */
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

            if (req.get("doctorId") == null || req.get("doctorId").isEmpty()) {
                return ResponseEntity.badRequest().body("doctorId is required");
            }

            if (req.get("startTime") == null || req.get("startTime").isEmpty()) {
                return ResponseEntity.badRequest().body("startTime is required");
            }

            UUID doctorId;
            try {
                doctorId = UUID.fromString(req.get("doctorId"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid doctorId format");
            }

            OffsetDateTime startTime;
            try {
                startTime = OffsetDateTime.parse(req.get("startTime"));
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body("Invalid date format. Use ISO-8601");
            }

            Doctor doctor = doctorRepo.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            // 🔥 NEW: Create appointment with PENDING status
            Appointment appt = Appointment.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .scheduledAt(startTime)
                    .status("PENDING")  // ⭐ PENDING - waiting for doctor confirmation
                    .notes(req.get("notes"))
                    .build();

            appointmentRepo.save(appt);

            // 🔥 Send notification emails
            // Patient: confirmation that request was sent
            emailService.sendAppointmentRequestToPatient(appt, patient, doctor);
            
            // Doctor: notification to confirm/reject
            emailService.sendAppointmentConfirmationRequestToDoctor(appt, patient, doctor);

            return ResponseEntity.ok(Map.of(
                    "message", "Appointment request sent to doctor",
                    "appointmentId", appt.getId(),
                    "status", "PENDING"
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/appointments/{id}/confirm
     * Doctor confirms appointment - Status: PENDING → CONFIRMED
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirmAppointment(
            @PathVariable UUID id,
            Authentication auth
    ) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"DOCTOR".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Only doctors can confirm");
            }

            Appointment appointment = appointmentRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            Doctor doctor = doctorRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Doctor profile missing"));

            // Verify this doctor owns the appointment
            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                return ResponseEntity.status(403).body("Not your appointment");
            }

            if (!"PENDING".equals(appointment.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Can only confirm pending appointments");
            }

            // 🔥 Update status to CONFIRMED
            appointment.setStatus("CONFIRMED");
            appointmentRepo.save(appointment);

            // 🔥 Send confirmation email to patient
            Patient patient = appointment.getPatient();
            emailService.sendAppointmentConfirmedToPatient(appointment, patient, doctor);

            return ResponseEntity.ok(Map.of(
                    "message", "Appointment confirmed",
                    "appointmentId", appointment.getId(),
                    "status", "CONFIRMED"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error confirming appointment: " + e.getMessage());
        }
    }

    /**
     * POST /api/appointments/{id}/reject
     * Doctor rejects appointment - Status: PENDING → CANCELLED
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectAppointment(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> req,
            Authentication auth
    ) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"DOCTOR".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Only doctors can reject");
            }

            Appointment appointment = appointmentRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            Doctor doctor = doctorRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Doctor profile missing"));

            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                return ResponseEntity.status(403).body("Not your appointment");
            }

            if (!"PENDING".equals(appointment.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Can only reject pending appointments");
            }

            String reason = req != null ? req.get("reason") : "Doctor declined the appointment";

            // 🔥 Update status to CANCELLED
            appointment.setStatus("CANCELLED");
            appointmentRepo.save(appointment);

            // 🔥 Send rejection email to patient
            Patient patient = appointment.getPatient();
            emailService.sendAppointmentRejectedToPatient(appointment, patient, doctor, reason);

            return ResponseEntity.ok(Map.of(
                    "message", "Appointment rejected",
                    "appointmentId", appointment.getId(),
                    "status", "CANCELLED",
                    "reason", reason
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error rejecting appointment: " + e.getMessage());
        }
    }

    /**
     * GET /api/appointments/my
     * Patient gets their appointments
     */
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
            appt.put("startTime", a.getScheduledAt());
            appt.put("status", a.getStatus());
            appt.put("notes", a.getNotes());

            Optional<Session> session = sessionRepo.findByAppointment(a);
            if (session.isPresent()) {
                appt.put("sessionId", session.get().getId());
                appt.put("sessionStatus", session.get().getStatus());
            } else {
                appt.put("sessionId", null);
                appt.put("sessionStatus", null);
            }

            Map<String, Object> doctor = new HashMap<>();
            doctor.put("id", a.getDoctor().getId());
            doctor.put("name", a.getDoctor().getFullName());
            doctor.put("specialization", a.getDoctor().getSpecialization());
            appt.put("doctor", doctor);

            response.add(appt);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/appointments/{id}/cancel
     * Either patient or doctor can cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> req,
            Authentication auth
    ) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Appointment appointment = appointmentRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            boolean isPatient = "PATIENT".equals(user.getRole());
            boolean isDoctor = "DOCTOR".equals(user.getRole());

            if (isPatient) {
                Patient patient = patientRepo.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException("Patient profile missing"));
                if (!appointment.getPatient().getId().equals(patient.getId())) {
                    return ResponseEntity.status(403).body("Not your appointment");
                }
            } else if (isDoctor) {
                Doctor doctor = doctorRepo.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException("Doctor profile missing"));
                if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                    return ResponseEntity.status(403).body("Not your appointment");
                }
            } else {
                return ResponseEntity.status(403).body("Access denied");
            }

            if ("COMPLETED".equals(appointment.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Cannot cancel a completed appointment");
            }

            if ("CANCELLED".equals(appointment.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Appointment is already cancelled");
            }

            // End session if in progress
            Optional<Session> existingSession = sessionRepo.findByAppointment(appointment);
            if (existingSession.isPresent() &&
                    "IN_PROGRESS".equals(existingSession.get().getStatus())) {
                Session session = existingSession.get();
                session.setStatus("ENDED");
                session.setEndedAt(OffsetDateTime.now());
                sessionRepo.save(session);
            }

            appointment.setStatus("CANCELLED");
            appointmentRepo.save(appointment);

            String reason = req != null ? req.get("reason") : "No reason provided";

            // 🔥 Send cancellation email to the other party
            Doctor doctor = appointment.getDoctor();
            Patient patient = appointment.getPatient();
            User doctorUser = doctor.getUser();
            User patientUser = patient.getUser();

            if (isDoctor) {
                // Doctor cancelled → notify patient
                emailService.sendAppointmentCancellation(
                        appointment, patientUser, doctorUser, reason
                );
            } else {
                // Patient cancelled → notify doctor
                emailService.sendAppointmentCancellation(
                        appointment, doctorUser, patientUser, reason
                );
            }

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
