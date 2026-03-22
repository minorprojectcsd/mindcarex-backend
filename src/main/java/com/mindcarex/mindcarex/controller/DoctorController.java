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
@CrossOrigin(origins = "*")
public class DoctorController {
    
    private final DoctorRepository doctorRepo;
    private final AppointmentRepository appointmentRepo;
    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    
    /**
     * GET /api/doctor/appointments
     * Returns doctor's appointments with full patient details and status
     */
    @GetMapping("/appointments")
    public ResponseEntity<?> myAppointments(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
                    
            if (!"DOCTOR".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            Doctor doctor = doctorRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Doctor profile missing"));
            
            List<Appointment> allAppointments = appointmentRepo.findByDoctor(doctor);
            
            // Sort: PENDING first, then CONFIRMED, then others
            allAppointments.sort((a1, a2) -> {
                int priority1 = getStatusPriority(a1.getStatus());
                int priority2 = getStatusPriority(a2.getStatus());
                if (priority1 != priority2) {
                    return Integer.compare(priority1, priority2);
                }
                return a1.getScheduledAt().compareTo(a2.getScheduledAt());
            });
            
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Appointment a : allAppointments) {
                Map<String, Object> appt = new HashMap<>();
                appt.put("id", a.getId());
                appt.put("startTime", a.getScheduledAt());
                appt.put("status", a.getStatus());
                appt.put("notes", a.getNotes());
                
                // Session info
                Optional<Session> session = sessionRepo.findByAppointment(a);
                if (session.isPresent()) {
                    appt.put("sessionId", session.get().getId());
                    appt.put("sessionStatus", session.get().getStatus());
                } else {
                    appt.put("sessionId", null);
                    appt.put("sessionStatus", null);
                }
                
                // Patient details
                Patient patient = a.getPatient();
                Map<String, Object> patientInfo = new HashMap<>();
                patientInfo.put("id", patient.getId());
                patientInfo.put("name", patient.getFullName());
                patientInfo.put("age", patient.getAge());
                patientInfo.put("gender", patient.getGender());
                patientInfo.put("phoneNumber", patient.getPhoneNumber());
                
                // Quick medical info for appointment list
                patientInfo.put("allergies", patient.getAllergies());
                patientInfo.put("currentMedications", patient.getCurrentMedications());
                
                appt.put("patient", patientInfo);
                
                response.add(appt);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch appointments: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/doctor/dashboard
     * Returns dashboard statistics including pending count
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
                    
            if (!"DOCTOR".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            Doctor doctor = doctorRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Doctor profile missing"));
            
            List<Appointment> allAppointments = appointmentRepo.findByDoctor(doctor);
            
            // Count by status
            long pendingCount = allAppointments.stream()
                    .filter(a -> "PENDING".equals(a.getStatus()))
                    .count();
                    
            long confirmedCount = allAppointments.stream()
                    .filter(a -> "CONFIRMED".equals(a.getStatus()))
                    .count();
                    
            long inProgressCount = allAppointments.stream()
                    .filter(a -> "IN_PROGRESS".equals(a.getStatus()))
                    .count();
                    
            long completedCount = allAppointments.stream()
                    .filter(a -> "COMPLETED".equals(a.getStatus()))
                    .count();
            
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("doctorName", doctor.getFullName());
            dashboard.put("specialization", doctor.getSpecialization());
            dashboard.put("pendingAppointments", pendingCount);
            dashboard.put("confirmedAppointments", confirmedCount);
            dashboard.put("inProgressSessions", inProgressCount);
            dashboard.put("completedSessions", completedCount);
            dashboard.put("totalAppointments", allAppointments.size());
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to load dashboard: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/doctor/pending
     * Returns only pending appointments (for quick access)
     */
    @GetMapping("/pending")
    public ResponseEntity<?> pendingAppointments(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
                    
            if (!"DOCTOR".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            Doctor doctor = doctorRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Doctor profile missing"));
            
            List<Appointment> pendingAppointments = appointmentRepo.findByDoctor(doctor).stream()
                    .filter(a -> "PENDING".equals(a.getStatus()))
                    .sorted(Comparator.comparing(Appointment::getScheduledAt))
                    .toList();
            
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Appointment a : pendingAppointments) {
                Map<String, Object> appt = new HashMap<>();
                appt.put("id", a.getId());
                appt.put("startTime", a.getScheduledAt());
                appt.put("notes", a.getNotes());
                
                Patient patient = a.getPatient();
                Map<String, Object> patientInfo = new HashMap<>();
                patientInfo.put("id", patient.getId());
                patientInfo.put("name", patient.getFullName());
                patientInfo.put("age", patient.getAge());
                patientInfo.put("gender", patient.getGender());
                
                appt.put("patient", patientInfo);
                response.add(appt);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch pending appointments: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to prioritize appointment statuses
     * Lower number = higher priority in list
     */
    private int getStatusPriority(String status) {
        return switch (status) {
            case "PENDING" -> 1;
            case "CONFIRMED" -> 2;
            case "IN_PROGRESS" -> 3;
            case "COMPLETED" -> 4;
            case "CANCELLED" -> 5;
            default -> 6;
        };
    }
}
