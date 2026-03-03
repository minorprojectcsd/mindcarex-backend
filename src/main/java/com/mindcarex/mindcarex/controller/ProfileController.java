package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication auth) {
        try {
            User user = userRepo.findByEmail(auth.getName()).orElseThrow();

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("role", user.getRole());

            if ("DOCTOR".equals(user.getRole())) {
                Doctor doctor = doctorRepo.findByUserId(user.getId()).orElse(null);  // ⭐ FIXED
                if (doctor != null) {
                    response.put("profile", buildDoctorResponse(doctor));
                }
            } else if ("PATIENT".equals(user.getRole())) {
                Patient patient = patientRepo.findByUserId(user.getId()).orElse(null);
                if (patient != null) {
                    response.put("profile", buildPatientResponse(patient));
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching profile");
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, Object> updates,
            Authentication auth
    ) {
        try {
            User user = userRepo.findByEmail(auth.getName()).orElseThrow();

            if (updates.containsKey("fullName")) {
                user.setFullName((String) updates.get("fullName"));
            }
            userRepo.save(user);

            if ("DOCTOR".equals(user.getRole())) {
                updateDoctorProfile(user.getId(), updates);
            } else if ("PATIENT".equals(user.getRole())) {
                updatePatientProfile(user.getId(), updates);
            }

            return ResponseEntity.ok("Profile updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating profile");
        }
    }

    private void updateDoctorProfile(UUID userId, Map<String, Object> updates) {
        Doctor doctor = doctorRepo.findByUserId(userId).orElseThrow();  // ⭐ FIXED

        if (updates.containsKey("specialization")) {
            doctor.setSpecialization((String) updates.get("specialization"));
        }
        if (updates.containsKey("licenseNumber")) {
            doctor.setLicenseNumber((String) updates.get("licenseNumber"));
        }
        if (updates.containsKey("experienceYears")) {
            doctor.setExperienceYears(parseInteger(updates.get("experienceYears")));
        }
        if (updates.containsKey("qualifications")) {
            doctor.setQualifications((String) updates.get("qualifications"));
        }
        if (updates.containsKey("bio")) {
            doctor.setBio((String) updates.get("bio"));
        }
        if (updates.containsKey("languages")) {
            doctor.setLanguages((String) updates.get("languages"));
        }
        if (updates.containsKey("clinicAddress")) {
            doctor.setClinicAddress((String) updates.get("clinicAddress"));
        }
        if (updates.containsKey("consultationFee")) {
            doctor.setConsultationFee((String) updates.get("consultationFee"));
        }

        doctorRepo.save(doctor);
    }

    private void updatePatientProfile(UUID userId, Map<String, Object> updates) {
        Patient patient = patientRepo.findByUserId(userId).orElseThrow();

        if (updates.containsKey("dateOfBirth")) {
            patient.setDateOfBirth(LocalDate.parse((String) updates.get("dateOfBirth")));
        }
        if (updates.containsKey("phoneNumber")) {
            patient.setPhoneNumber((String) updates.get("phoneNumber"));
        }
        if (updates.containsKey("gender")) {
            patient.setGender((String) updates.get("gender"));
        }
        if (updates.containsKey("address")) {
            patient.setAddress((String) updates.get("address"));
        }
        if (updates.containsKey("emergencyContactName")) {
            patient.setEmergencyContactName((String) updates.get("emergencyContactName"));
        }
        if (updates.containsKey("emergencyContactPhone")) {
            patient.setEmergencyContactPhone((String) updates.get("emergencyContactPhone"));
        }
        if (updates.containsKey("emergencyContactEmail")) {
            patient.setEmergencyContactEmail((String) updates.get("emergencyContactEmail"));
        }
        if (updates.containsKey("emergencyContactRelation")) {
            patient.setEmergencyContactRelation((String) updates.get("emergencyContactRelation"));
        }
        if (updates.containsKey("medicalHistory")) {
            patient.setMedicalHistory((String) updates.get("medicalHistory"));
        }
        if (updates.containsKey("bloodGroup")) {
            patient.setBloodGroup((String) updates.get("bloodGroup"));
        }
        if (updates.containsKey("allergies")) {
            patient.setAllergies((String) updates.get("allergies"));
        }
        if (updates.containsKey("currentMedications")) {
            patient.setCurrentMedications((String) updates.get("currentMedications"));
        }
        if (updates.containsKey("insuranceProvider")) {
            patient.setInsuranceProvider((String) updates.get("insuranceProvider"));
        }
        if (updates.containsKey("insuranceNumber")) {
            patient.setInsuranceNumber((String) updates.get("insuranceNumber"));
        }

        patientRepo.save(patient);
    }

    private Map<String, Object> buildDoctorResponse(Doctor doctor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", doctor.getId());
        map.put("specialization", doctor.getSpecialization());
        map.put("licenseNumber", doctor.getLicenseNumber());
        map.put("experienceYears", doctor.getExperienceYears());
        map.put("qualifications", doctor.getQualifications());
        map.put("bio", doctor.getBio());
        map.put("languages", doctor.getLanguages());
        map.put("clinicAddress", doctor.getClinicAddress());
        map.put("consultationFee", doctor.getConsultationFee());
        map.put("availabilityStatus", doctor.getAvailabilityStatus());
        return map;
    }

    private Map<String, Object> buildPatientResponse(Patient patient) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", patient.getId());
        map.put("dateOfBirth", patient.getDateOfBirth());
        map.put("phoneNumber", patient.getPhoneNumber());
        map.put("gender", patient.getGender());
        map.put("age", patient.getAge());
        map.put("address", patient.getAddress());
        map.put("emergencyContactName", patient.getEmergencyContactName());
        map.put("emergencyContactPhone", patient.getEmergencyContactPhone());
        map.put("emergencyContactEmail", patient.getEmergencyContactEmail());
        map.put("emergencyContactRelation", patient.getEmergencyContactRelation());
        map.put("medicalHistory", patient.getMedicalHistory());
        map.put("bloodGroup", patient.getBloodGroup());
        map.put("allergies", patient.getAllergies());
        map.put("currentMedications", patient.getCurrentMedications());
        map.put("insuranceProvider", patient.getInsuranceProvider());
        map.put("insuranceNumber", patient.getInsuranceNumber());
        return map;
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}