package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.Doctor;
import com.mindcarex.mindcarex.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final DoctorRepository doctorRepository;
    
    /**
     * GET /api/users/doctors
     * Returns all doctors with complete information for patient selection
     * Accessible by: PATIENT, DOCTOR, ADMIN (any authenticated user)
     */
    @GetMapping("/doctors")
    public ResponseEntity<?> getDoctors(Authentication auth) {
        try {
            List<Doctor> doctors = doctorRepository.findAll();
            
            // 🔥 DEBUG LOG
            System.out.println("=== FETCHING DOCTORS FOR PATIENT ===");
            System.out.println("Total doctors found: " + doctors.size());
            
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Doctor d : doctors) {
                // 🔥 DEBUG: Log each doctor
                System.out.println("\nDoctor: " + d.getFullName());
                System.out.println("  Specialization: " + d.getSpecialization());
                System.out.println("  Experience: " + d.getExperienceYears() + " years");
                System.out.println("  Clinic: " + d.getClinicAddress());
                System.out.println("  Fee: " + d.getConsultationFee());
                System.out.println("  Bio: " + (d.getBio() != null ? d.getBio().substring(0, Math.min(50, d.getBio().length())) + "..." : "null"));
                
                Map<String, Object> doctor = new HashMap<>();
                
                // ===== BASIC INFO =====
                doctor.put("id", d.getId());
                doctor.put("name", d.getFullName());
                
                // Email (from User entity)
                if (d.getUser() != null) {
                    doctor.put("email", d.getUser().getEmail());
                } else {
                    doctor.put("email", null);
                    System.out.println("  ⚠️ WARNING: User relationship is NULL for doctor " + d.getId());
                }
                
                // ===== PROFESSIONAL INFO =====
                doctor.put("specialization", d.getSpecialization());
                doctor.put("licenseNumber", d.getLicenseNumber());
                doctor.put("experienceYears", d.getExperienceYears());
                
                // ===== DETAILED INFO =====
                doctor.put("qualifications", d.getQualifications());
                doctor.put("bio", d.getBio());
                doctor.put("languages", d.getLanguages());
                
                // ===== LOCATION & PRICING =====
                doctor.put("clinicAddress", d.getClinicAddress());
                doctor.put("consultationFee", d.getConsultationFee());
                
                // ===== AVAILABILITY =====
                doctor.put("availabilityStatus", d.getAvailabilityStatus());
                
                response.add(doctor);
            }
            
            System.out.println("\n✅ Returning " + response.size() + " doctors to frontend");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ ERROR fetching doctors: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of(
                    "error", "Failed to fetch doctors",
                    "message", e.getMessage()
                ));
        }
    }
}
