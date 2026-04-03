package com.abhishek.notix.sms_sender_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.sms_sender_service.config.SmsConfig;
import com.abhishek.notix.sms_sender_service.model.DeliveryLog;
import com.abhishek.notix.sms_sender_service.repo.NotificationRepository;
import com.abhishek.notix.sms_sender_service.repo.DeliveryLogRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SmsSenderService {

    @Autowired
    private DeliveryLogRepository logRepo;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SmsConfig smsConfig;


    @Transactional
    public void sendSms(NotificationEvent event) {
        Optional<DeliveryLog> existingLog = logRepo.findByNotificationIdAndAttemptNo(event.getId(), event.getAttemptNo());
        if (existingLog.isPresent() && existingLog.get().getStatus() != Status.PENDING) {
            return;
        }

        DeliveryLog log = existingLog.orElseGet(() ->
                new DeliveryLog(event.getId(), event.getAttemptNo(), Status.PENDING, null, Instant.now()));

        try {
            Message.creator(
                    new PhoneNumber(event.getTo()),
                    smsConfig.getFromNumber(),
                    event.getTemplate() // or format a message with event.getParams()
            ).create();

            log.setStatus(Status.SENT);
            log.setErrorMessage(null);
            log.setTimestamp(Instant.now());
            logRepo.save(log);
            updateNotificationStatus(event.getId(), Status.SENT);

            System.out.println("✅ SMS sent to " + event.getTo());

        } catch (Exception e) {
            log.setStatus(Status.FAILED);
            log.setErrorMessage(e.getMessage());
            log.setTimestamp(Instant.now());
            logRepo.save(log);
            updateNotificationStatus(event.getId(), Status.FAILED);

            System.err.println("❌ Failed to send SMS: " + e.getMessage());
        }
    }

    private void updateNotificationStatus(UUID notificationId, Status status) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(status);
            notificationRepository.save(notification);
        });
    }
}
