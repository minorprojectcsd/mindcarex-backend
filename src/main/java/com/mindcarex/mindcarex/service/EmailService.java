package com.mindcarex.mindcarex.service;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.EmailLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:https://mindcarex.vercel.app}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a");

    /**
     * Send appointment confirmation to patient
     */
    @Async
    public void sendAppointmentConfirmationToPatient(
            Appointment appointment,
            Patient patient,
            Doctor doctor
    ) {
        try {
            User patientUser = patient.getUser();
            User doctorUser = doctor.getUser();

            Context context = new Context();
            context.setVariable("patientName", patientUser.getFullName());
            context.setVariable("doctorName", doctorUser.getFullName());
            context.setVariable("specialization", doctor.getSpecialization());
            context.setVariable("appointmentDate",
                    appointment.getScheduledAt().format(DATE_FORMATTER));
            context.setVariable("appointmentTime",
                    appointment.getScheduledAt().format(TIME_FORMATTER));
            context.setVariable("appointmentId", appointment.getId().toString().substring(0, 8));
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");
            context.setVariable("notes", appointment.getNotes());

            String htmlContent = templateEngine.process(
                    "email/appointment-booked-patient",
                    context
            );

            sendEmail(
                    patientUser.getEmail(),
                    "Appointment Confirmed - mindcareX",
                    htmlContent,
                    "APPOINTMENT_BOOKED",
                    appointment.getId(),
                    null
            );

            log.info("Sent appointment confirmation to patient: {}", patientUser.getEmail());

        } catch (Exception e) {
            log.error("Failed to send appointment confirmation to patient", e);
        }
    }

    /**
     * Send appointment notification to doctor
     */
    @Async
    public void sendAppointmentNotificationToDoctor(
            Appointment appointment,
            Patient patient,
            Doctor doctor
    ) {
        try {
            User patientUser = patient.getUser();
            User doctorUser = doctor.getUser();

            Context context = new Context();
            context.setVariable("doctorName", doctorUser.getFullName());
            context.setVariable("patientName", patientUser.getFullName());
            context.setVariable("appointmentDate",
                    appointment.getScheduledAt().format(DATE_FORMATTER));
            context.setVariable("appointmentTime",
                    appointment.getScheduledAt().format(TIME_FORMATTER));
            context.setVariable("appointmentId", appointment.getId().toString().substring(0, 8));
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");
            context.setVariable("notes", appointment.getNotes());

            String htmlContent = templateEngine.process(
                    "email/appointment-booked-doctor",
                    context
            );

            sendEmail(
                    doctorUser.getEmail(),
                    "New Appointment Scheduled - mindcareX",
                    htmlContent,
                    "APPOINTMENT_BOOKED",
                    appointment.getId(),
                    null
            );

            log.info("Sent appointment notification to doctor: {}", doctorUser.getEmail());

        } catch (Exception e) {
            log.error("Failed to send appointment notification to doctor", e);
        }
    }

    /**
     * Send 10-minute reminder before session
     */
    @Async
    public void sendSessionReminder(
            Appointment appointment,
            User recipient,
            User otherParticipant,
            UUID sessionId
    ) {
        try {
            Context context = new Context();
            context.setVariable("recipientName", recipient.getFullName());
            context.setVariable("otherParticipant", otherParticipant.getFullName());
            context.setVariable("appointmentDate",
                    appointment.getScheduledAt().format(DATE_FORMATTER));
            context.setVariable("appointmentTime",
                    appointment.getScheduledAt().format(TIME_FORMATTER));
            context.setVariable("sessionUrl",
                    frontendUrl + "/video-session/" + sessionId);

            String htmlContent = templateEngine.process(
                    "email/session-reminder",
                    context
            );

            sendEmail(
                    recipient.getEmail(),
                    "⏰ Session Starting in 10 Minutes - mindcareX",
                    htmlContent,
                    "SESSION_REMINDER",
                    appointment.getId(),
                    sessionId
            );

            log.info("Sent session reminder to: {}", recipient.getEmail());

        } catch (Exception e) {
            log.error("Failed to send session reminder", e);
        }
    }

    /**
     * Send appointment cancellation notification
     */
    @Async
    public void sendAppointmentCancellation(
            Appointment appointment,
            User recipient,
            User otherParticipant,
            String cancellationReason
    ) {
        try {
            Context context = new Context();
            context.setVariable("recipientName", recipient.getFullName());
            context.setVariable("otherParticipant", otherParticipant.getFullName());
            context.setVariable("appointmentDate",
                    appointment.getScheduledAt().format(DATE_FORMATTER));
            context.setVariable("appointmentTime",
                    appointment.getScheduledAt().format(TIME_FORMATTER));
            context.setVariable("cancellationReason", cancellationReason);
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

            String htmlContent = templateEngine.process(
                    "email/appointment-cancelled",
                    context
            );

            sendEmail(
                    recipient.getEmail(),
                    "Appointment Cancelled - mindcareX",
                    htmlContent,
                    "APPOINTMENT_CANCELLED",
                    appointment.getId(),
                    null
            );

            log.info("Sent cancellation notification to: {}", recipient.getEmail());

        } catch (Exception e) {
            log.error("Failed to send cancellation notification", e);
        }
    }

    /**
     * Send session summary after completion
     */
    @Async
    public void sendSessionSummary(
            Session session,
            User recipient,
            String aiSummary,
            List<String> keyPoints,
            String recommendations,
            String nextSteps
    ) {
        try {
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

            String htmlContent = templateEngine.process(
                    "email/session-summary",
                    context
            );

            sendEmail(
                    recipient.getEmail(),
                    "Session Summary - mindcareX",
                    htmlContent,
                    "SESSION_SUMMARY",
                    session.getAppointment().getId(),
                    session.getId()
            );

            log.info("Sent session summary to: {}", recipient.getEmail());

        } catch (Exception e) {
            log.error("Failed to send session summary", e);
        }
    }
    @Async
    public void sendSessionSummaryToGuardian(
            Session session,
            Patient patient,
            String aiSummary,
            List<String> keyPoints,
            String recommendations
    ) {
        try {
            if (patient.getEmergencyContactEmail() == null ||
                    patient.getEmergencyContactEmail().isEmpty()) {
                log.info("No guardian email for patient: {}", patient.getId());
                return;
            }

            User patientUser = patient.getUser();

            Context context = new Context();
            context.setVariable("recipientName", patient.getEmergencyContactName());
            context.setVariable("patientName", patientUser.getFullName());
            context.setVariable("relation", patient.getEmergencyContactRelation());
            context.setVariable("duration", calculateDuration(session));
            context.setVariable("sessionDate",
                    session.getStartedAt().format(DATE_FORMATTER));
            context.setVariable("aiSummary", aiSummary);
            context.setVariable("keyPoints", keyPoints);
            context.setVariable("recommendations", recommendations);
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

            String htmlContent = templateEngine.process(
                    "email/guardian-session-summary",
                    context
            );

            sendEmail(
                    patient.getEmergencyContactEmail(),
                    "Session Update for " + patientUser.getFullName() + " - mindcareX",
                    htmlContent,
                    "GUARDIAN_NOTIFICATION",
                    session.getAppointment().getId(),
                    session.getId()
            );

            log.info("Sent session summary to guardian: {}",
                    patient.getEmergencyContactEmail());

        } catch (Exception e) {
            log.error("Failed to send guardian notification", e);
        }
    }

    /**
     * Core email sending method
     */
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "mindcareX");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            emailLog.setStatus("SENT");
            emailLog.setSentAt(OffsetDateTime.now());

        } catch (Exception e) {
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        } finally {
            emailLogRepository.save(emailLog);
        }
    }

    /**
     * Calculate session duration
     */
    private String calculateDuration(Session session) {
        if (session.getEndedAt() == null) {
            return "In Progress";
        }

        long minutes = java.time.Duration.between(
                session.getStartedAt(),
                session.getEndedAt()
        ).toMinutes();

        return minutes + " min";
    }

    /**
     * Get email history for a user
     */
    public List<EmailLog> getEmailHistory(String email) {
        return emailLogRepository.findByRecipientOrderByCreatedAtDesc(email);
    }

    /**
     * Get email statistics
     */
    public Map<String, Long> getEmailStatistics() {
        return Map.of(
                "total", emailLogRepository.count(),
                "sent", emailLogRepository.countByStatus("SENT"),
                "failed", emailLogRepository.countByStatus("FAILED"),
                "pending", emailLogRepository.countByStatus("PENDING")
        );
    }
}