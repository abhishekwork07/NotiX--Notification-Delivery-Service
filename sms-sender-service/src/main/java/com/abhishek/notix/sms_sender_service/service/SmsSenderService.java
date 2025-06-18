package com.abhishek.notix.sms_sender_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.sms_sender_service.model.DeliveryLog;
import com.abhishek.notix.sms_sender_service.repo.DeliveryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SmsSenderService {

    @Autowired
    private DeliveryLogRepository logRepo;


    @KafkaListener(topics = "${kafka.consumer.sms-topic}", groupId = "${spring.application.name}")
    public void sendSms(NotificationEvent event) {
        if (logRepo.existsByNotificationIdAndAttemptNo(event.getId(), 1)) return;

        try {
            // 🟡 Mock SMS send (log to console)
            System.out.printf("📲 Sending SMS to %s with template: %s%n", event.getTo(), event.getTemplate());

            logRepo.save(new DeliveryLog(event.getId(), 1, Status.SENT, null, Instant.now()));

        } catch (Exception e) {
            logRepo.save(new DeliveryLog(event.getId(), 1, Status.FAILED, e.getMessage(), Instant.now()));
        }
    }
}

