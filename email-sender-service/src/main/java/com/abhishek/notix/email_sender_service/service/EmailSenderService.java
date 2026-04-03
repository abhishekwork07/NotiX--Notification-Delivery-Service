package com.abhishek.notix.email_sender_service.service;

import com.abhishek.notix.email_sender_service.config.MailConfig;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.email_sender_service.model.DeliveryLog;
import com.abhishek.notix.email_sender_service.model.Notification;
import com.abhishek.notix.email_sender_service.repo.DeliveryLogRepository;
import com.abhishek.notix.email_sender_service.repo.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class EmailSenderService {

    @Autowired
    private DeliveryLogRepository logRepo;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MailConfig mailConfig;

    @Transactional
    public void sendEmail(NotificationEvent event) {
        Optional<DeliveryLog> existingLog = logRepo.findByNotificationIdAndAttemptNo(event.getId(), event.getAttemptNo());
        if (existingLog.isPresent() && existingLog.get().getStatus() != Status.PENDING) {
            return;
        }

        DeliveryLog log = existingLog.orElseGet(() ->
                new DeliveryLog(event.getId(), event.getAttemptNo(), Status.PENDING, null, Instant.now()));

        try {
            // 📨 Mock email send
            // SimpleMailMessage message = new SimpleMailMessage();
            // message.setTo(event.getTo());
            // message.setSubject("Notification: " + event.getTemplate());
            // message.setText(event.getParams().toString());
            // mailSender.send(message);

            log.setStatus(Status.SENT);
            log.setErrorMessage(null);
            log.setTimestamp(Instant.now());
            logRepo.save(log);
            updateNotificationStatus(event.getId(), Status.SENT);

        } catch (Exception ex) {
            log.setStatus(Status.FAILED);
            log.setErrorMessage(ex.getMessage());
            log.setTimestamp(Instant.now());
            logRepo.save(log);
            updateNotificationStatus(event.getId(), Status.FAILED);
        }
    }

    private void updateNotificationStatus(java.util.UUID notificationId, Status status) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(status);
            notificationRepository.save(notification);
        });
    }
}
