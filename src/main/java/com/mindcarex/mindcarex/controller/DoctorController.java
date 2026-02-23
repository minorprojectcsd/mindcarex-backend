package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepo;
    private final AppointmentRepository appointmentRepo;
    private final UserRepository userRepo;
    private final SessionRepository sessionRepo; // ⭐ NEW

    // =========================
    // DOCTOR → MY APPOINTMENTS (⭐ UPDATED)
    // =========================
    @GetMapping("/appointments")
    public ResponseEntity<?> myAppointments(Authentication auth) {

        User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"DOCTOR".equals(user.getRole())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        Doctor doctor = doctorRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Doctor profile missing"));

        List<Map<String, Object>> response = new ArrayList<>();

        for (Appointment a : appointmentRepo.findByDoctor(doctor)) {

            Map<String, Object> appt = new HashMap<>();
            appt.put("id", a.getId());
            appt.put("startTime", a.getStartTime());
            appt.put("endTime", a.getEndTime());
            appt.put("status", a.getStatus());

            // ⭐ NEW: Include session info if exists
            Optional<Session> session = sessionRepo.findByAppointment(a);
            if (session.isPresent()) {
                appt.put("sessionId", session.get().getId());
                appt.put("sessionStatus", session.get().getStatus());
            } else {
                appt.put("sessionId", null);
                appt.put("sessionStatus", null);
            }

            // PATIENT DETAILS
            Map<String, Object> patient = new HashMap<>();
            patient.put("id", a.getPatient().getId());
            patient.put("name", a.getPatient().getFullName());

            appt.put("patient", patient);

            response.add(appt);
        }

        return ResponseEntity.ok(response);
    }
}