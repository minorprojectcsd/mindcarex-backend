package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.*;
import com.mindcarex.mindcarex.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");
        String role = req.get("role");
        String fullName = req.get("fullName");

        if (email == null || password == null || role == null || fullName == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        // 1️⃣ Create USER
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);

        // 2️⃣ Create ROLE PROFILE
        if ("DOCTOR".equalsIgnoreCase(role)) {

            String specialization = req.getOrDefault("specialization", "GENERAL");
            String licenseNumber = req.getOrDefault(
                    "licenseNumber",
                    "LIC-" + UUID.randomUUID()
            );

            Doctor doctor = new Doctor();
            doctor.setUserId(user.getId());
            doctor.setFullName(fullName);
            doctor.setSpecialization(specialization);
            doctor.setLicenseNumber(licenseNumber);

            doctorRepository.save(doctor);
        }

        if ("PATIENT".equalsIgnoreCase(role)) {

            LocalDate dob = req.get("dateOfBirth") != null
                    ? LocalDate.parse(req.get("dateOfBirth"))
                    : LocalDate.now();

            Patient patient = new Patient();
            patient.setUserId(user.getId());
            patient.setFullName(fullName);
            patient.setDateOfBirth(dob);

            patientRepository.save(patient);
        }

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // ⭐ CHANGED: Added userId and email to response
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole(),
                "email", user.getEmail(),
                "userId", user.getId().toString() // ⭐ NEW
        ));
    }
}