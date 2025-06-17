package com.abhishek.notix.email_sender_service.service;

import com.abhishek.notix.email_sender_service.config.MailConfig;
import com.abhishek.notix.email_sender_service.dto.NotificationEvent;
import com.abhishek.notix.email_sender_service.enums.Status;
import com.abhishek.notix.email_sender_service.model.DeliveryLog;
import com.abhishek.notix.email_sender_service.repo.DeliveryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EmailSenderService {

    @Autowired
    private DeliveryLogRepository logRepo;

//    @Autowired
//    private final JavaMailSender mailSender;

    @Autowired
    private MailConfig mailConfig;

    private JavaMailSender getJavaMailSender() {
        return mailConfig.javaMailSender();
    }

//    public EmailSenderService(DeliveryLogRepository logRepo, JavaMailSender mailSender) {
//        this.logRepo = logRepo;
//        this.mailSender = mailSender;
//    }

    @KafkaListener(topics = "${kafka.consumer.email-topic}", groupId = "${spring.application.name}")
    public void sendEmail(NotificationEvent event) {
        // ✅ Idempotency check: we don't resend if already attempted
        boolean alreadyProcessed = logRepo.existsByNotificationIdAndAttemptNo(event.getId(), 1);
        if (alreadyProcessed) return;

        try {
            // 📨 Mock email send
            // SimpleMailMessage message = new SimpleMailMessage();
            // message.setTo(event.getTo());
            // message.setSubject("Notification: " + event.getTemplate());
            // message.setText(event.getParams().toString());
            // mailSender.send(message);

            // ✅ Log successful attempt
            DeliveryLog log = new DeliveryLog(event.getId(), 1, Status.SENT, null, Instant.now());
            logRepo.save(log);

        } catch (Exception ex) {
            // ❌ Log failure for retry or DLQ
            DeliveryLog log = new DeliveryLog(event.getId(), 1, Status.FAILED, ex.getMessage(), Instant.now());
            logRepo.save(log);
        }
    }
}

