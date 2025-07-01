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
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
            try {
                // Fetch original notification data
                Notification notification = notificationRepository
                        .findById(log.getNotificationId())
                        .orElseThrow(() -> new RuntimeException("Notification not found for retry"));

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
                }
            }

            deliveryLogRepository.save(log);
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

