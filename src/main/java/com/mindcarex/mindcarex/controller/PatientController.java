package com.mindcarex.mindcarex.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "Patient dashboard accessed";
    }
}
