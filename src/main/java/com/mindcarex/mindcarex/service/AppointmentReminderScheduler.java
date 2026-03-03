package com.mindcarex.mindcarex.service;

import com.mindcarex.mindcarex.entity.*;
import com.mindcarex.mindcarex.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final SessionRepository sessionRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 * * * * *")  // Every minute
    public void sendUpcomingAppointmentReminders() {
        log.debug("Checking for upcoming appointments...");

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime tenMinutesFromNow = now.plusMinutes(10);
        OffsetDateTime elevenMinutesFromNow = now.plusMinutes(11);

        List<Appointment> upcomingAppointments = appointmentRepository
                .findByScheduledAtBetweenAndStatus(
                        tenMinutesFromNow,
                        elevenMinutesFromNow,
                        "BOOKED"
                );

        for (Appointment appointment : upcomingAppointments) {
            try {
                UUID sessionId = findOrCreateSessionId(appointment);

                Doctor doctor = appointment.getDoctor();
                Patient patient = appointment.getPatient();
                User doctorUser = doctor.getUser();
                User patientUser = patient.getUser();

                emailService.sendSessionReminder(
                        appointment,
                        doctorUser,
                        patientUser,
                        sessionId
                );

                emailService.sendSessionReminder(
                        appointment,
                        patientUser,
                        doctorUser,
                        sessionId
                );

                log.info("Sent reminders for appointment: {}", appointment.getId());

            } catch (Exception e) {
                log.error("Failed to send reminder for appointment: {}",
                        appointment.getId(), e);
            }
        }
    }

    private UUID findOrCreateSessionId(Appointment appointment) {
        return sessionRepository.findByAppointment(appointment)
                .map(Session::getId)
                .orElse(appointment.getId());
    }
}