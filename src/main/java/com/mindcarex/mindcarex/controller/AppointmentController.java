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

    // ✅ CREATE → now PENDING (updated)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> req, Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"PATIENT".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Only patients can book");
            }

            Patient patient = patientRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Patient profile missing"));

            UUID doctorId = UUID.fromString(req.get("doctorId"));
            OffsetDateTime startTime = OffsetDateTime.parse(req.get("startTime"));

            Doctor doctor = doctorRepo.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            Appointment appt = Appointment.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .scheduledAt(startTime)
                    .status("PENDING") // 🔥 changed
                    .notes(req.get("notes"))
                    .build();

            appointmentRepo.save(appt);

            // 🔥 updated emails
            emailService.sendAppointmentRequestToPatient(appt, patient, doctor);
            emailService.sendAppointmentConfirmationRequestToDoctor(appt, patient, doctor);

            return ResponseEntity.ok(Map.of(
                    "message", "Appointment request sent",
                    "appointmentId", appt.getId(),
                    "status", "PENDING"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ NEW: CONFIRM
    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(@PathVariable UUID id, Authentication auth) {
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

            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                return ResponseEntity.status(403).body("Not your appointment");
            }

            if (!"PENDING".equals(appointment.getStatus())) {
                return ResponseEntity.badRequest().body("Only pending appointments can be confirmed");
            }

            appointment.setStatus("CONFIRMED");
            appointmentRepo.save(appointment);

            emailService.sendAppointmentConfirmedToPatient(appointment, appointment.getPatient(), doctor);

            return ResponseEntity.ok(Map.of("status", "CONFIRMED"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ✅ NEW: REJECT
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id,
                                   @RequestBody(required = false) Map<String, String> req,
                                   Authentication auth) {
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

            String reason = req != null ? req.get("reason") : "Doctor declined";

            appointment.setStatus("CANCELLED");
            appointmentRepo.save(appointment);

            emailService.sendAppointmentRejectedToPatient(
                    appointment, appointment.getPatient(), doctor, reason
            );

            return ResponseEntity.ok(Map.of("status", "CANCELLED"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ✅ SAME (unchanged)
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

            Optional<Session> session = sessionRepo.findByAppointment(a);
            appt.put("sessionId", session.map(Session::getId).orElse(null));
            appt.put("sessionStatus", session.map(Session::getStatus).orElse(null));

            Map<String, Object> doctor = new HashMap<>();
            doctor.put("id", a.getDoctor().getId());
            doctor.put("name", a.getDoctor().getFullName());
            doctor.put("specialization", a.getDoctor().getSpecialization());

            appt.put("doctor", doctor);

            response.add(appt);
        }

        return ResponseEntity.ok(response);
    }

    // ✅ FIXED cancel (important bug fix)
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

            if (!isPatient && !isDoctor) {
                return ResponseEntity.status(403).body("Access denied");
            }

            appointment.setStatus("CANCELLED");
            appointmentRepo.save(appointment);

            return ResponseEntity.ok(Map.of("status", "CANCELLED"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
