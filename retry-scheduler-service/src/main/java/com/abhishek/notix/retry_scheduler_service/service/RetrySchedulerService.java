package com.abhishek.notix.retry_scheduler_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.retry_scheduler_service.model.DeadLetter;
import com.abhishek.notix.retry_scheduler_service.model.DeliveryLog;
import com.abhishek.notix.retry_scheduler_service.model.Notification;
import com.abhishek.notix.retry_scheduler_service.repo.DeadLetterRepository;
import com.abhishek.notix.retry_scheduler_service.repo.DeliveryLogRepository;
import com.abhishek.notix.retry_scheduler_service.repo.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    @Value("${kafka.producer.email-topic}")
    private String emailTopic;

    @Value("${kafka.producer.sms-topic}")
    private String smsTopic;

    private static final int MAX_ATTEMPTS = 3;

    //@Timed(value = "retry.process.time", description = "Time taken to process retries")
    @Scheduled(fixedDelay = 15000)
    public void retryFailedMessages() {
        List<DeliveryLog> failedLogs = deliveryLogRepository.findRetryableMessages(MAX_ATTEMPTS);

        for (DeliveryLog log : failedLogs) {
            // Fetch original notification data
            Notification notification = notificationRepository
                    .findById(log.getNotificationId())
                    .orElseThrow(() -> new RuntimeException("Notification not found for retry"));
            try {
                // Rebuild the event with original data
                NotificationEvent event = buildNotificationEvent(notification);

                // Publish again to Kafka
                kafkaTemplate.send("notifications", notification.getId().toString(), event);

                // Update log status
                log.setAttemptNo(log.getAttemptNo() + 1);
                log.setStatus(Status.PENDING);
                log.setErrorMessage(null);
            } catch (Exception ex) {
                log.setAttemptNo(log.getAttemptNo() + 1);
                log.setErrorMessage(ex.getMessage());

                if (log.getAttemptNo() >= MAX_ATTEMPTS) {
                    log.setStatus(Status.FAILED); // Move to DLQ or stop retrying
                    persistToDLQ(log, notification);
                }
            }

            deliveryLogRepository.save(log);
        }
    }

    private void persistToDLQ(DeliveryLog log, Notification notification) {
        try {
            DeadLetter dlq = new DeadLetter();
            dlq.setId(log.getNotificationId());
            dlq.setRecipient(notification.getRecipient());
            dlq.setChannel(notification.getChannel());
            dlq.setStatus(log.getStatus());
            dlq.setTemplate("testing");
            dlq.setErrorMessage(log.getErrorMessage());
            dlq.setCreatedAt(Instant.now());

            deadLetterRepository.save(dlq);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to save to DLQ: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000) // Every 60 seconds
    public void moveFailedLogsToDLQ() {
        List<DeliveryLog> failedLogs = deliveryLogRepository.findByStatus(Status.FAILED);


        for (DeliveryLog log : failedLogs) {
            try {
                // Fetch original notification data
                Notification notification = notificationRepository
                        .findById(log.getNotificationId())
                        .orElseThrow(() -> new RuntimeException("Notification not found for retry"));
                persistToDLQ(log, notification); // Use existing method
                //deliveryLogRepository.delete(log); // Optional: delete after moving
                System.out.println("✅ Moved to DLQ: " + log.getNotificationId());
            } catch (Exception e) {
                System.err.println("⚠️ Error moving to DLQ: " + e.getMessage());
            }
        }
    }



    private NotificationEvent buildNotificationEvent(Notification notification) {
        return NotificationEvent.builder()
                .id(notification.getId())
                .to(notification.getRecipient())
                .channel(notification.getChannel())
                .template(notification.getTemplate())
                .build();
    }
}

