package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.*;
import com.mindcarex.mindcarex.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Enhanced registration with profile fields
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> requestBody) {
        try {
            String email = (String) requestBody.get("email");
            String password = (String) requestBody.get("password");
            String fullName = (String) requestBody.get("fullName");
            String role = (String) requestBody.get("role");

            // Validate required fields
            if (email == null || password == null || fullName == null || role == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            // Check if user already exists
            if (userRepo.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body("Email already registered");
            }

            // Create user
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .role(role.toUpperCase())
                    .enabled(true)
                    .build();

            userRepo.save(user);

            // Create role-specific profile
            if ("DOCTOR".equalsIgnoreCase(role)) {
                createDoctorProfile(user, requestBody);
            } else if ("PATIENT".equalsIgnoreCase(role)) {
                createPatientProfile(user, requestBody);
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(
                    user.getEmail(),
                    user.getRole()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("role", user.getRole());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Create doctor profile with enhanced fields
     */
    private void createDoctorProfile(User user, Map<String, Object> requestBody) {
        Doctor doctor = Doctor.builder()
                .userId(user.getId())
                .specialization((String) requestBody.get("specialization"))
                .licenseNumber((String) requestBody.get("licenseNumber"))
                .experienceYears(parseInteger(requestBody.get("experienceYears")))
                .qualifications((String) requestBody.get("qualifications"))
                .bio((String) requestBody.get("bio"))
                .languages((String) requestBody.get("languages"))
                .clinicAddress((String) requestBody.get("clinicAddress"))
                .consultationFee((String) requestBody.get("consultationFee"))
                .availabilityStatus("AVAILABLE")
                .build();

        doctorRepo.save(doctor);
    }

    /**
     * Create patient profile with enhanced fields
     */
    private void createPatientProfile(User user, Map<String, Object> requestBody) {
        // Parse date of birth
        LocalDate dob = null;
        if (requestBody.get("dateOfBirth") != null) {
            String dobStr = (String) requestBody.get("dateOfBirth");
            dob = LocalDate.parse(dobStr);
        }

        Patient patient = Patient.builder()
                .userId(user.getId())
                .dateOfBirth(dob)
                .phoneNumber((String) requestBody.get("phoneNumber"))
                .gender((String) requestBody.get("gender"))
                .address((String) requestBody.get("address"))
                .emergencyContactName((String) requestBody.get("emergencyContactName"))
                .emergencyContactPhone((String) requestBody.get("emergencyContactPhone"))
                .emergencyContactEmail((String) requestBody.get("emergencyContactEmail"))
                .emergencyContactRelation((String) requestBody.get("emergencyContactRelation"))
                .medicalHistory((String) requestBody.get("medicalHistory"))
                .bloodGroup((String) requestBody.get("bloodGroup"))
                .allergies((String) requestBody.get("allergies"))
                .currentMedications((String) requestBody.get("currentMedications"))
                .insuranceProvider((String) requestBody.get("insuranceProvider"))
                .insuranceNumber((String) requestBody.get("insuranceNumber"))
                .build();

        patientRepo.save(patient);
    }

    /**
     * Helper to parse integer safely
     */
    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");

            // Authenticate
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // Get user
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate token
            String token = jwtUtil.generateToken(
                    user.getEmail(),
                    user.getRole()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("role", user.getRole());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}