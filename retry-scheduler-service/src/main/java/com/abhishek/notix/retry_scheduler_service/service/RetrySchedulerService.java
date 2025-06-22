package com.abhishek.notix.retry_scheduler_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.retry_scheduler_service.model.DeadLetter;
import com.abhishek.notix.retry_scheduler_service.model.DeliveryLog;
import com.abhishek.notix.retry_scheduler_service.repo.DeadLetterRepository;
import com.abhishek.notix.retry_scheduler_service.repo.DeliveryLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RetrySchedulerService {

    private final DeliveryLogRepository logRepo;
    private final DeadLetterRepository dlqRepo;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${kafka.producer.email-topic}")
    private String emailTopic;

    @Value("${kafka.producer.sms-topic}")
    private String smsTopic;

    private static final int MAX_ATTEMPTS = 3;

    public RetrySchedulerService(DeliveryLogRepository logRepo,
                                 DeadLetterRepository dlqRepo,
                                 KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.logRepo = logRepo;
        this.dlqRepo = dlqRepo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 30000) // every 30 seconds
    public void retryFailedMessages() {
        List<DeliveryLog> failedLogs = logRepo.findByStatusAndAttemptNoLessThan(Status.FAILED, MAX_ATTEMPTS);

        for (DeliveryLog log : failedLogs) {
            try {
                // You should ideally fetch the NotificationEvent again from notification table or cache
                // Here we simulate republishing
                NotificationEvent event = new NotificationEvent(
                        log.getNotificationId(),
                        "test@domain.com", // placeholder
                        Channel.EMAIL,     // placeholder
                        "WELCOME",         // placeholder
                        Map.of()           // placeholder
                );

                String topic = event.getChannel() == Channel.EMAIL ? emailTopic : smsTopic;
                kafkaTemplate.send(topic, event.getId().toString(), event);

                log.setAttemptNo(log.getAttemptNo() + 1);
                logRepo.save(log);
            } catch (Exception ex) {
                // Push to dead-letter queue after max attempts
                if (log.getAttemptNo() >= MAX_ATTEMPTS - 1) {
                    DeadLetter dlq = new DeadLetter();
                    dlq.setId(log.getNotificationId());
                    dlq.setRecipient("unknown");
                    dlq.setTemplate("unknown");
                    dlq.setChannel(Channel.EMAIL);
                    dlq.setErrorMessage("Max retries exceeded");
                    dlqRepo.save(dlq);
                }
            }
        }
    }
}

