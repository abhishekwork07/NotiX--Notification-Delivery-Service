package com.abhishek.notix.retry_scheduler_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
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
            DeadLetter dlq = new DeadLetter();
            dlq.setId(log.getNotificationId());
            dlq.setRecipient(notification.getRecipient());
            dlq.setChannel(notification.getChannel());
            dlq.setStatus(log.getStatus());
            dlq.setTemplate(notification.getTemplate());
            dlq.setErrorMessage(log.getErrorMessage());
            dlq.setCreatedAt(Instant.now());

            deadLetterRepository.save(dlq);
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

                notification.setStatus(Status.FAILED);
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
                .to(notification.getRecipient())
                .channel(notification.getChannel())
                .template(notification.getTemplate())
                .attemptNo(attemptNo)
                .build();
    }
}
