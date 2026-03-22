package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.Appointment;
import com.mindcarex.mindcarex.entity.Doctor;
import com.mindcarex.mindcarex.entity.Patient;
import com.mindcarex.mindcarex.entity.User;
import com.mindcarex.mindcarex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PatientController {
    
    private final UserRepository userRepo;
    private final PatientRepository patientRepo;
    private final DoctorRepository doctorRepo;
    private final AppointmentRepository appointmentRepo;
    private final SessionRepository sessionRepo;
    
    /**
     * GET /api/patient/dashboard
     * Returns patient dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"PATIENT".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            Patient patient = patientRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile missing"));
            
            // Get upcoming appointments
            List<Appointment> upcomingAppointments = appointmentRepo
                .findByPatient(patient).stream()
                .filter(a -> "BOOKED".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus()))
                .toList();
            
            // Get completed appointments count
            long completedCount = appointmentRepo.findByPatient(patient).stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
            
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("patientName", patient.getFullName());
            dashboard.put("upcomingAppointmentsCount", upcomingAppointments.size());
            dashboard.put("completedSessionsCount", completedCount);
            dashboard.put("hasEmergencyContact", patient.getEmergencyContactEmail() != null);
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to load dashboard: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/patient/profile
     * Returns patient's own profile details
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"PATIENT".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            Patient patient = patientRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile missing"));
            
            Map<String, Object> profile = new HashMap<>();
            
            // Basic Info
            profile.put("id", patient.getId());
            profile.put("name", patient.getFullName());
            profile.put("email", user.getEmail());
            
            // Personal Info
            profile.put("dateOfBirth", patient.getDateOfBirth());
            profile.put("age", patient.getAge());
            profile.put("gender", patient.getGender());
            profile.put("phoneNumber", patient.getPhoneNumber());
            profile.put("address", patient.getAddress());
            
            // Medical Info
            profile.put("bloodGroup", patient.getBloodGroup());
            profile.put("allergies", patient.getAllergies());
            profile.put("medicalHistory", patient.getMedicalHistory());
            profile.put("currentMedications", patient.getCurrentMedications());
            
            // Emergency Contact
            Map<String, String> emergencyContact = new HashMap<>();
            emergencyContact.put("name", patient.getEmergencyContactName());
            emergencyContact.put("phone", patient.getEmergencyContactPhone());
            emergencyContact.put("email", patient.getEmergencyContactEmail());
            emergencyContact.put("relation", patient.getEmergencyContactRelation());
            profile.put("emergencyContact", emergencyContact);
            
            // Insurance
            Map<String, String> insurance = new HashMap<>();
            insurance.put("provider", patient.getInsuranceProvider());
            insurance.put("number", patient.getInsuranceNumber());
            profile.put("insurance", insurance);
            
            return ResponseEntity.ok(profile);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch profile: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/patient/appointments
     * Returns patient's appointments with full doctor details
     */
    @GetMapping("/appointments")
    public ResponseEntity<?> getAppointments(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"PATIENT".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            Patient patient = patientRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile missing"));
            
            List<Map<String, Object>> appointments = new ArrayList<>();
            
            for (Appointment a : appointmentRepo.findByPatient(patient)) {
                Map<String, Object> appt = new HashMap<>();
                appt.put("id", a.getId());
                appt.put("scheduledAt", a.getScheduledAt());
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
                
                // Doctor details
                Doctor doctor = a.getDoctor();
                Map<String, Object> doctorInfo = new HashMap<>();
                doctorInfo.put("id", doctor.getId());
                doctorInfo.put("name", doctor.getFullName());
                doctorInfo.put("specialization", doctor.getSpecialization());
                doctorInfo.put("experienceYears", doctor.getExperienceYears());
                doctorInfo.put("clinicAddress", doctor.getClinicAddress());
                doctorInfo.put("consultationFee", doctor.getConsultationFee());
                
                appt.put("doctor", doctorInfo);
                
                appointments.add(appt);
            }
            
            return ResponseEntity.ok(appointments);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch appointments: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/patient/doctors
     * Returns list of all available doctors for booking
     */
    @GetMapping("/doctors")
    public ResponseEntity<?> getAvailableDoctors(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"PATIENT".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            List<Doctor> doctors = doctorRepo.findAll();
            
            List<Map<String, Object>> doctorList = new ArrayList<>();
            
            for (Doctor d : doctors) {
                Map<String, Object> doctor = new HashMap<>();
                
                // Basic Info
                doctor.put("id", d.getId());
                doctor.put("name", d.getFullName());
                doctor.put("email", d.getUser() != null ? d.getUser().getEmail() : null);
                
                // Professional Info
                doctor.put("specialization", d.getSpecialization());
                doctor.put("licenseNumber", d.getLicenseNumber());
                doctor.put("experienceYears", d.getExperienceYears());
                
                // Detailed Info
                doctor.put("qualifications", d.getQualifications());
                doctor.put("bio", d.getBio());
                doctor.put("languages", d.getLanguages());
                
                // Location & Fees
                doctor.put("clinicAddress", d.getClinicAddress());
                doctor.put("consultationFee", d.getConsultationFee());
                
                // Availability
                doctor.put("availabilityStatus", d.getAvailabilityStatus());
                
                doctorList.add(doctor);
            }
            
            return ResponseEntity.ok(doctorList);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch doctors: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/patient/statistics
     * Returns patient statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"PATIENT".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            Patient patient = patientRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile missing"));
            
            List<Appointment> allAppointments = appointmentRepo.findByPatient(patient);
            
            long totalAppointments = allAppointments.size();
            long completedSessions = allAppointments.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
            long upcomingSessions = allAppointments.stream()
                .filter(a -> "BOOKED".equals(a.getStatus()))
                .count();
            long cancelledSessions = allAppointments.stream()
                .filter(a -> "CANCELLED".equals(a.getStatus()))
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAppointments", totalAppointments);
            stats.put("completedSessions", completedSessions);
            stats.put("upcomingSessions", upcomingSessions);
            stats.put("cancelledSessions", cancelledSessions);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch statistics: " + e.getMessage()));
        }
    }
}
