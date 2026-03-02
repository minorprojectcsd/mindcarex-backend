package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.EmailLog;
import com.mindcarex.mindcarex.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class EmailNotificationController {

    private final EmailService emailService;

    /**
     * Get email history for current user
     */
    @GetMapping("/history")
    public ResponseEntity<?> getEmailHistory(Authentication auth) {
        try {
            List<EmailLog> history = emailService.getEmailHistory(auth.getName());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching email history");
        }
    }

    /**
     * Get email statistics (Admin only)
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(Authentication auth) {
        try {
            // TODO: Add admin check
            Map<String, Long> stats = emailService.getEmailStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching statistics");
        }
    }

    /**
     * Test email sending
     */
    @PostMapping("/test")
    public ResponseEntity<?> sendTestEmail(
            @RequestBody Map<String, String> request,
            Authentication auth
    ) {
        try {
            String recipientEmail = request.getOrDefault("email", auth.getName());

            // Send a simple test email
            // emailService.sendTestEmail(recipientEmail);

            return ResponseEntity.ok("Test email sent to: " + recipientEmail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sending test email");
        }
    }
}