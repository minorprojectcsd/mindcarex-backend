package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final DoctorRepository doctorRepository;

    @GetMapping("/doctors")
    public List<Map<String, Object>> getDoctors() {

        List<Map<String, Object>> response = new ArrayList<>();

        doctorRepository.findAll().forEach(d -> {
            Map<String, Object> doctor = new HashMap<>();
            doctor.put("id", d.getId());
            doctor.put("name", d.getFullName());
            doctor.put("specialization", d.getSpecialization());
            response.add(doctor);
        });

        return response;
    }
}
