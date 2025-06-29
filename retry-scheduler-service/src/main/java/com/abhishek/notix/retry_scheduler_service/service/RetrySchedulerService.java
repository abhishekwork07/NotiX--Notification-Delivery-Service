package com.abhishek.notix.retry_scheduler_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.retry_scheduler_service.model.DeadLetter;
import com.abhishek.notix.retry_scheduler_service.model.DeliveryLog;
import com.abhishek.notix.retry_scheduler_service.repo.DeadLetterRepository;
import com.abhishek.notix.retry_scheduler_service.repo.DeliveryLogRepository;
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

    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${kafka.producer.email-topic}")
    private String emailTopic;

    @Value("${kafka.producer.sms-topic}")
    private String smsTopic;

    private static final int MAX_ATTEMPTS = 3;

    @Timed(value = "retry.process.time", description = "Time taken to process retries")
    @Scheduled(fixedDelay = 15000)
    public void retryFailedMessages() {
        List<DeliveryLog> failedLogs = deliveryLogRepository.findRetryableMessages(MAX_ATTEMPTS);

        for (DeliveryLog log : failedLogs) {
            try {
                // Re-publish to Kafka
                kafkaTemplate.send("notifications", log.getNotificationId().toString(), buildNotificationEvent(log));

                log.setAttemptNo(log.getAttemptNo() + 1);
                log.setStatus(Status.PENDING); // Set to PENDING again
                log.setErrorMessage(null);
            } catch (Exception ex) {
                log.setAttemptNo(log.getAttemptNo() + 1);
                log.setErrorMessage(ex.getMessage());

                if (log.getAttemptNo() >= MAX_ATTEMPTS) {
                    log.setStatus(Status.FAILED); // Simulate DLQ
                }
            }

            deliveryLogRepository.save(log);
        }
    }

    private NotificationEvent buildNotificationEvent(DeliveryLog log) {
        // You'll need to reconstruct from delivery log, or add required data for retry
        return new NotificationEvent(); // Implement your mapping
    }
}

