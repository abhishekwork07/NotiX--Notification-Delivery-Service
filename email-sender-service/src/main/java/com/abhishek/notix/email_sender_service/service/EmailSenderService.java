package com.abhishek.notix.email_sender_service.service;

import com.abhishek.notix.email_sender_service.config.MailConfig;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.dto.NotificationStatusEvent;
import com.abhishek.notix.common.enums.NotificationLifecycleEventType;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.email_sender_service.model.DeliveryLog;
import com.abhishek.notix.email_sender_service.model.Notification;
import com.abhishek.notix.email_sender_service.model.ProviderAccount;
import com.abhishek.notix.email_sender_service.repo.DeliveryLogRepository;
import com.abhishek.notix.email_sender_service.repo.NotificationRepository;
import com.abhishek.notix.email_sender_service.repo.ProviderAccountRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
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

    @Autowired
    private ProviderAccountRepository providerAccountRepository;

    @Autowired
    private KafkaTemplate<String, NotificationStatusEvent> notificationStatusKafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public void sendEmail(NotificationEvent event) {
        Optional<DeliveryLog> existingLog = logRepo.findByNotificationIdAndAttemptNo(event.getId(), event.getAttemptNo());
        if (existingLog.isPresent() && existingLog.get().getStatus() != Status.PENDING) {
            return;
        }

        DeliveryLog log = existingLog.orElseGet(() -> new DeliveryLog(event.getId(), event.getAttemptNo(), Status.PENDING, null, Instant.now()));
        log.setTenantId(event.getTenantId());
        log.setChannel(event.getChannel());
        log.setProviderAccountId(event.getProviderAccountId());

        try {
            JavaMailSender activeSender = resolveSender(event);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getTo());
            message.setSubject(event.getSubject() != null ? event.getSubject() : "Notification: " + event.getTemplate());
            message.setText(event.getBody() != null ? event.getBody() : String.valueOf(event.getParams()));
            activeSender.send(message);

            log.setStatus(Status.SENT);
            log.setErrorMessage(null);
            log.setTimestamp(Instant.now());
            logRepo.save(log);
            updateNotificationStatus(event.getId(), Status.SENT);
            publishStatusEvent(event, Status.SENT, NotificationLifecycleEventType.SENT, null);

        } catch (Exception ex) {
            log.setStatus(Status.FAILED);
            log.setErrorMessage(ex.getMessage());
            log.setTimestamp(Instant.now());
            logRepo.save(log);
            updateNotificationStatus(event.getId(), Status.FAILED);
            publishStatusEvent(event, Status.FAILED, NotificationLifecycleEventType.FAILED, ex.getMessage());
        }
    }

    private void updateNotificationStatus(java.util.UUID notificationId, Status status) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(status);
            notificationRepository.save(notification);
        });
    }

    private JavaMailSender resolveSender(NotificationEvent event) {
        if (event.getProviderAccountId() == null || event.getTenantId() == null) {
            return mailSender;
        }

        ProviderAccount provider = providerAccountRepository.findByIdAndTenantId(event.getProviderAccountId(), event.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Provider account not found for email notification"));

        try {
            Map<String, Object> config = objectMapper.readValue(provider.getConfigurationJson(), new TypeReference<>() {
            });
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(String.valueOf(config.getOrDefault("host", "")));
            Object port = config.get("port");
            if (port != null) {
                sender.setPort(Integer.parseInt(String.valueOf(port)));
            }
            sender.setUsername(String.valueOf(config.getOrDefault("username", "")));
            sender.setPassword(String.valueOf(config.getOrDefault("password", "")));
            return sender.getHost() == null || sender.getHost().isBlank() ? mailSender : sender;
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid SMTP provider configuration", ex);
        }
    }

    private void publishStatusEvent(NotificationEvent event, Status status,
                                    NotificationLifecycleEventType eventType, String errorMessage) {
        NotificationStatusEvent statusEvent = new NotificationStatusEvent(
                event.getTenantId(),
                event.getId(),
                event.getChannel(),
                status,
                eventType,
                event.getAttemptNo(),
                errorMessage,
                Instant.now()
        );
        notificationStatusKafkaTemplate.send("notifications.status", event.getId().toString(), statusEvent);
    }
}
