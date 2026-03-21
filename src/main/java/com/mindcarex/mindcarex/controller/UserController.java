package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.Doctor;
import com.mindcarex.mindcarex.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final DoctorRepository doctorRepository;
    
    @GetMapping("/doctors")
    public ResponseEntity<?> getDoctors() {
        try {
            List<Map<String, Object>> response = new ArrayList<>();
            
            doctorRepository.findAll().forEach(d -> {
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
                
                response.add(doctor);
            });
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch doctors: " + e.getMessage()));
        }
    }
}
