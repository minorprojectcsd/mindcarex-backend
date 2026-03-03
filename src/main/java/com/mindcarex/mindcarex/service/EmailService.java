package com.mindcarex.mindcarex.service;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    @Value("${BREVO_FROM_EMAIL}")
    private String fromEmail;

    @Value("${app.frontend.url:https://mindcarex.vercel.app}")
    private String frontendUrl;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.brevo.com/v3")
            .build();

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a");

    /* ============================================================
       CORE BREVO EMAIL SENDER
       ============================================================ */

    @Async
    private void sendEmail(
            String to,
            String subject,
            String htmlContent,
            String emailType,
            UUID appointmentId,
            UUID sessionId
    ) {

        EmailLog emailLog = EmailLog.builder()
                .recipient(to)
                .subject(subject)
                .emailType(emailType)
                .status("PENDING")
                .appointmentId(appointmentId)
                .sessionId(sessionId)
                .build();

        try {

            Map<String, Object> body = Map.of(
                    "sender", Map.of(
                            "name", "mindcareX",
                            "email", fromEmail
                    ),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            webClient.post()
                    .uri("/smtp/email")
                    .header("api-key", brevoApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            emailLog.setStatus("SENT");
            emailLog.setSentAt(OffsetDateTime.now());

        } catch (Exception e) {
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            log.error("Brevo email failed: {}", e.getMessage());
        }

        emailLogRepository.save(emailLog);
    }

    /* ============================================================
       APPOINTMENT METHODS
       ============================================================ */

    @Async
    public void sendAppointmentConfirmationToPatient(
            Appointment appointment,
            Patient patient,
            Doctor doctor
    ) {

        Context context = new Context();
        context.setVariable("patientName", patient.getUser().getFullName());
        context.setVariable("doctorName", doctor.getUser().getFullName());
        context.setVariable("specialization", doctor.getSpecialization());
        context.setVariable("appointmentDate",
                appointment.getScheduledAt().format(DATE_FORMATTER));
        context.setVariable("appointmentTime",
                appointment.getScheduledAt().format(TIME_FORMATTER));
        context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

        String htmlContent =
                templateEngine.process("email/appointment-booked-patient", context);

        sendEmail(
                patient.getUser().getEmail(),
                "Appointment Confirmed - mindcareX",
                htmlContent,
                "APPOINTMENT_BOOKED",
                appointment.getId(),
                null
        );
    }

    @Async
    public void sendAppointmentNotificationToDoctor(
            Appointment appointment,
            Patient patient,
            Doctor doctor
    ) {

        Context context = new Context();
        context.setVariable("doctorName", doctor.getUser().getFullName());
        context.setVariable("patientName", patient.getUser().getFullName());
        context.setVariable("appointmentDate",
                appointment.getScheduledAt().format(DATE_FORMATTER));
        context.setVariable("appointmentTime",
                appointment.getScheduledAt().format(TIME_FORMATTER));
        context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

        String htmlContent =
                templateEngine.process("email/appointment-booked-doctor", context);

        sendEmail(
                doctor.getUser().getEmail(),
                "New Appointment Scheduled - mindcareX",
                htmlContent,
                "APPOINTMENT_BOOKED",
                appointment.getId(),
                null
        );
    }

    /* ============================================================
       SESSION METHODS
       ============================================================ */

    @Async
    public void sendSessionReminder(
            Appointment appointment,
            User recipient,
            User otherParticipant,
            UUID sessionId
    ) {

        Context context = new Context();
        context.setVariable("recipientName", recipient.getFullName());
        context.setVariable("otherParticipant", otherParticipant.getFullName());
        context.setVariable("appointmentDate",
                appointment.getScheduledAt().format(DATE_FORMATTER));
        context.setVariable("appointmentTime",
                appointment.getScheduledAt().format(TIME_FORMATTER));
        context.setVariable("sessionUrl",
                frontendUrl + "/video-session/" + sessionId);

        String htmlContent =
                templateEngine.process("email/session-reminder", context);

        sendEmail(
                recipient.getEmail(),
                "Session Starting Soon - mindcareX",
                htmlContent,
                "SESSION_REMINDER",
                appointment.getId(),
                sessionId
        );
    }

    @Async
    public void sendSessionSummary(
            Session session,
            User recipient,
            String aiSummary,
            List<String> keyPoints,
            String recommendations,
            String nextSteps
    ) {

        Context context = new Context();
        context.setVariable("recipientName", recipient.getFullName());
        context.setVariable("duration", calculateDuration(session));
        context.setVariable("sessionDate",
                session.getStartedAt().format(DATE_FORMATTER));
        context.setVariable("aiSummary", aiSummary);
        context.setVariable("keyPoints", keyPoints);
        context.setVariable("recommendations", recommendations);
        context.setVariable("nextSteps", nextSteps);
        context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

        String htmlContent =
                templateEngine.process("email/session-summary", context);

        sendEmail(
                recipient.getEmail(),
                "Session Summary - mindcareX",
                htmlContent,
                "SESSION_SUMMARY",
                session.getAppointment().getId(),
                session.getId()
        );
    }


    @Async
    public void sendAppointmentCancellation(
            Appointment appointment,
            User recipient,
            User otherParticipant,
            String cancellationReason
    ) {

        Context context = new Context();
        context.setVariable("recipientName", recipient.getFullName());
        context.setVariable("otherParticipant", otherParticipant.getFullName());
        context.setVariable("appointmentDate",
                appointment.getScheduledAt().format(DATE_FORMATTER));
        context.setVariable("appointmentTime",
                appointment.getScheduledAt().format(TIME_FORMATTER));
        context.setVariable("cancellationReason", cancellationReason);
        context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

        String htmlContent =
                templateEngine.process("email/appointment-cancelled", context);

        sendEmail(
                recipient.getEmail(),
                "Appointment Cancelled - mindcareX",
                htmlContent,
                "APPOINTMENT_CANCELLED",
                appointment.getId(),
                null
        );
    }

    @Async
    public void sendSessionSummaryToGuardian(
            Session session,
            Patient patient,
            String aiSummary,
            List<String> keyPoints,
            String recommendations
    ) {

        if (patient.getEmergencyContactEmail() == null) return;

        Context context = new Context();
        context.setVariable("recipientName", patient.getEmergencyContactName());
        context.setVariable("patientName", patient.getUser().getFullName());
        context.setVariable("relation", patient.getEmergencyContactRelation());
        context.setVariable("duration", calculateDuration(session));
        context.setVariable("sessionDate",
                session.getStartedAt().format(DATE_FORMATTER));
        context.setVariable("aiSummary", aiSummary);
        context.setVariable("keyPoints", keyPoints);
        context.setVariable("recommendations", recommendations);
        context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

        String htmlContent =
                templateEngine.process("email/guardian-session-summary", context);

        sendEmail(
                patient.getEmergencyContactEmail(),
                "Session Update for " + patient.getUser().getFullName(),
                htmlContent,
                "GUARDIAN_NOTIFICATION",
                session.getAppointment().getId(),
                session.getId()
        );
    }

    @Async
    public void sendTestEmail(String recipientEmail) {

        try {

            Context context = new Context();
            context.setVariable("recipientName", recipientEmail);
            context.setVariable("timestamp",
                    OffsetDateTime.now().format(
                            DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")
                    ));
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

            String htmlContent =
                    templateEngine.process("email/test-email", context);

            sendEmail(
                    recipientEmail,
                    "Test Email - mindcareX",
                    htmlContent,
                    "TEST_EMAIL",
                    null,
                    null
            );

            log.info("Test email sent to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send test email", e);
        }
    }

    /* ============================================================
       RESEND + STATS
       ============================================================ */

    @Async
    public void resendEmail(EmailLog emailLog) {

        String htmlContent = templateEngine.process(
                "email/generic-resend",
                new Context()
        );

        sendEmail(
                emailLog.getRecipient(),
                emailLog.getSubject(),
                htmlContent,
                emailLog.getEmailType(),
                emailLog.getAppointmentId(),
                emailLog.getSessionId()
        );
    }

    private String calculateDuration(Session session) {

        if (session.getEndedAt() == null) return "In Progress";

        long minutes = java.time.Duration.between(
                session.getStartedAt(),
                session.getEndedAt()
        ).toMinutes();

        return minutes + " min";
    }

    public List<EmailLog> getEmailHistory(String email) {
        return emailLogRepository.findByRecipientOrderByCreatedAtDesc(email);
    }

    public Map<String, Long> getEmailStatistics() {
        return Map.of(
                "total", emailLogRepository.count(),
                "sent", emailLogRepository.countByStatus("SENT"),
                "failed", emailLogRepository.countByStatus("FAILED"),
                "pending", emailLogRepository.countByStatus("PENDING")
        );
    }
}