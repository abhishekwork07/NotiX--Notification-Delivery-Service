package com.abhishek.notix.retry_scheduler_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.dto.NotificationStatusEvent;
import com.abhishek.notix.common.enums.NotificationLifecycleEventType;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.retry_scheduler_service.model.DeadLetter;
import com.abhishek.notix.retry_scheduler_service.model.DeliveryLog;
import com.abhishek.notix.retry_scheduler_service.model.Notification;
import com.abhishek.notix.retry_scheduler_service.repo.DeadLetterRepository;
import com.abhishek.notix.retry_scheduler_service.repo.DeliveryLogRepository;
import com.abhishek.notix.retry_scheduler_service.repo.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RetrySchedulerService {

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Autowired
    private KafkaTemplate<String, NotificationStatusEvent> notificationStatusKafkaTemplate;

    private static final int MAX_ATTEMPTS = 3;

    @Transactional
    @Scheduled(fixedDelay = 15000)
    public void retryFailedMessages() {
        List<DeliveryLog> failedLogs = deliveryLogRepository.findLatestFailedAttempts(MAX_ATTEMPTS);

        for (DeliveryLog log : failedLogs) {
            Notification notification = notificationRepository
                    .findById(log.getNotificationId())
                    .orElseThrow(() -> new RuntimeException("Notification not found for retry"));

            int nextAttempt = log.getAttemptNo() + 1;
            if (deliveryLogRepository.existsByNotificationIdAndAttemptNo(notification.getId(), nextAttempt)) {
                continue;
            }

            DeliveryLog retryLog = new DeliveryLog(
                    notification.getId(),
                    nextAttempt,
                    Status.PENDING,
                    null,
                    Instant.now()
            );
            retryLog.setTenantId(notification.getTenantId());
            retryLog.setChannel(notification.getChannel());
            retryLog.setProviderAccountId(notification.getProviderAccountId());
            deliveryLogRepository.save(retryLog);

            try {
                notification.setStatus(Status.PENDING);
                notificationRepository.save(notification);

                NotificationEvent event = buildNotificationEvent(notification, nextAttempt);
                kafkaTemplate.send("notifications", notification.getId().toString(), event);
            } catch (Exception ex) {
                retryLog.setStatus(Status.FAILED);
                retryLog.setErrorMessage(ex.getMessage());
                retryLog.setTimestamp(Instant.now());
                deliveryLogRepository.save(retryLog);

                notification.setStatus(Status.FAILED);
                notificationRepository.save(notification);

                if (nextAttempt >= MAX_ATTEMPTS) {
                    persistToDLQ(retryLog, notification);
                }
            }
        }
    }

    private void persistToDLQ(DeliveryLog log, Notification notification) {
        try {
            notification.setStatus(Status.DEAD_LETTERED);
            notificationRepository.save(notification);

            DeadLetter dlq = new DeadLetter();
            dlq.setId(log.getNotificationId());
            dlq.setTenantId(notification.getTenantId());
            dlq.setRecipient(notification.getRecipient());
            dlq.setChannel(notification.getChannel());
            dlq.setStatus(Status.DEAD_LETTERED);
            dlq.setTemplate(notification.getTemplate());
            dlq.setErrorMessage(log.getErrorMessage());
            dlq.setCreatedAt(Instant.now());

            deadLetterRepository.save(dlq);
            publishStatusEvent(notification, log.getAttemptNo(), Status.DEAD_LETTERED,
                    NotificationLifecycleEventType.DEAD_LETTERED, log.getErrorMessage());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to save to DLQ: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000) // Every 60 seconds
    public void moveFailedLogsToDLQ() {
        List<DeliveryLog> failedLogs = deliveryLogRepository.findTerminalFailures(MAX_ATTEMPTS);

        for (DeliveryLog log : failedLogs) {
            try {
                Notification notification = notificationRepository
                        .findById(log.getNotificationId())
                        .orElseThrow(() -> new RuntimeException("Notification not found for DLQ"));

                notification.setStatus(Status.DEAD_LETTERED);
                notificationRepository.save(notification);
                persistToDLQ(log, notification);
                System.out.println("✅ Moved to DLQ: " + log.getNotificationId());
            } catch (Exception e) {
                System.err.println("⚠️ Error moving to DLQ: " + e.getMessage());
            }
        }
    }

    private NotificationEvent buildNotificationEvent(Notification notification, int attemptNo) {
        return NotificationEvent.builder()
                .id(notification.getId())
                .tenantId(notification.getTenantId())
                .to(notification.getRecipient())
                .channel(notification.getChannel())
                .template(notification.getTemplate())
                .templateId(notification.getTemplateId())
                .idempotencyKey(notification.getIdempotencyKey())
                .providerAccountId(notification.getProviderAccountId())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .scheduledAt(notification.getScheduledAt())
                .attemptNo(attemptNo)
                .build();
    }

    private void publishStatusEvent(Notification notification, int attemptNo, Status status,
                                    NotificationLifecycleEventType eventType, String errorMessage) {
        NotificationStatusEvent statusEvent = new NotificationStatusEvent(
                notification.getTenantId(),
                notification.getId(),
                notification.getChannel(),
                status,
                eventType,
                attemptNo,
                errorMessage,
                Instant.now()
        );
        notificationStatusKafkaTemplate.send("notifications.status", notification.getId().toString(), statusEvent);
    }
}
