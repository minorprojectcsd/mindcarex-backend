package com.mindcarex.mindcarex.repository;

import com.mindcarex.mindcarex.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {

    List<EmailLog> findByRecipientOrderByCreatedAtDesc(String recipient);

    List<EmailLog> findByAppointmentIdOrderByCreatedAtDesc(UUID appointmentId);

    List<EmailLog> findByStatusOrderByCreatedAtDesc(String status);

    Long countByStatusAndEmailType(String status, String emailType);
}