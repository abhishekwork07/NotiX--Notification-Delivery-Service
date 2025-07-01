package com.abhishek.notix.sms_sender_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.sms_sender_service.config.SmsConfig;
import com.abhishek.notix.sms_sender_service.model.DeliveryLog;
import com.abhishek.notix.sms_sender_service.repo.DeliveryLogRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SmsSenderService {

    @Autowired
    private DeliveryLogRepository logRepo;

    @Autowired
    private SmsConfig smsConfig;


    public void sendSms(NotificationEvent event) {
        try {
            Message.creator(
                    new PhoneNumber(event.getTo()),
                    smsConfig.getFromNumber(),
                    event.getTemplate() // or format a message with event.getParams()
            ).create();

            logRepo.save(
                    DeliveryLog.builder()
                            .notificationId(event.getId())
                            .status(Status.SENT)
                            .attemptNo(1)
                            .timestamp(Instant.now())
                            .build()
            );

            System.out.println("✅ SMS sent to " + event.getTo());

        } catch (Exception e) {
            logRepo.save(
                    DeliveryLog.builder()
                            .notificationId(event.getId())
                            .status(Status.FAILED)
                            .attemptNo(1)
                            .errorMessage(e.getMessage())
                            .timestamp(Instant.now())
                            .build()
            );

            System.err.println("❌ Failed to send SMS: " + e.getMessage());
        }
    }
}

