package com.mindcarex.mindcarex.controller;

import com.mindcarex.mindcarex.entity.EmailLog;
import com.mindcarex.mindcarex.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class EmailNotificationController {

    private final EmailService emailService;
    private final EmailLogRepository emailLogRepository;

    /**
     * Get email history for current user
     */
    @GetMapping("/history")
    public ResponseEntity<?> getEmailHistory(Authentication auth) {
        List<EmailLog> history =
                emailService.getEmailHistory(auth.getName());
        return ResponseEntity.ok(history);
    }

    /**
     * Get email statistics (ADMIN only)
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(Authentication auth) {

        if (!auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).body("Access denied");
        }

        return ResponseEntity.ok(emailService.getEmailStatistics());
    }

    /**
     * Resend failed email
     */
    @PostMapping("/{logId}/resend")
    public ResponseEntity<?> resendEmail(
            @PathVariable UUID logId,
            Authentication auth
    ) {

        EmailLog emailLog = emailLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Email log not found"));

        // Only recipient or admin can resend
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!emailLog.getRecipient().equals(auth.getName()) && !isAdmin) {
            return ResponseEntity.status(403).body("Access denied");
        }

        if (!"FAILED".equals(emailLog.getStatus())) {
            return ResponseEntity.badRequest()
                    .body("Only FAILED emails can be resent");
        }

        // Call resend method
        emailService.resendEmail(emailLog);

        return ResponseEntity.ok("Email resent successfully");
    }

    /**
     * Send test email
     */
    @PostMapping("/test")
    public ResponseEntity<?> sendTestEmail(
            @RequestBody(required = false) Map<String, String> request,
            Authentication auth
    ) {

        String recipientEmail =
                request != null && request.containsKey("email")
                        ? request.get("email")
                        : auth.getName();

        emailService.sendTestEmail(recipientEmail);

        return ResponseEntity.ok("Test email sent to: " + recipientEmail);
    }
}