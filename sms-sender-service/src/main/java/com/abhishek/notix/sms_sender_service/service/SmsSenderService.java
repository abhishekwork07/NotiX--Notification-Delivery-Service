package com.abhishek.notix.sms_sender_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.dto.NotificationStatusEvent;
import com.abhishek.notix.common.enums.NotificationLifecycleEventType;
import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.sms_sender_service.config.SmsConfig;
import com.abhishek.notix.sms_sender_service.model.DeliveryLog;
import com.abhishek.notix.sms_sender_service.model.ProviderAccount;
import com.abhishek.notix.sms_sender_service.repo.NotificationRepository;
import com.abhishek.notix.sms_sender_service.repo.DeliveryLogRepository;
import com.abhishek.notix.sms_sender_service.repo.ProviderAccountRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
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

    @Autowired
    private ProviderAccountRepository providerAccountRepository;

    @Autowired
    private KafkaTemplate<String, NotificationStatusEvent> notificationStatusKafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public void sendSms(NotificationEvent event) {
        Optional<DeliveryLog> existingLog = logRepo.findByNotificationIdAndAttemptNo(event.getId(), event.getAttemptNo());
        if (existingLog.isPresent() && existingLog.get().getStatus() != Status.PENDING) {
            return;
        }

        DeliveryLog log = existingLog.orElseGet(() ->
                new DeliveryLog(event.getId(), event.getAttemptNo(), Status.PENDING, null, Instant.now()));
        log.setTenantId(event.getTenantId());
        log.setChannel(event.getChannel());
        log.setProviderAccountId(event.getProviderAccountId());

        try {
            TwilioConfig twilioConfig = resolveTwilioConfig(event);
            Twilio.init(twilioConfig.accountSid(), twilioConfig.authToken());
            Message.creator(
                    new PhoneNumber(event.getTo()),
                    new PhoneNumber(twilioConfig.fromNumber()),
                    event.getBody() != null ? event.getBody() : event.getTemplate()
            ).create();

            log.setStatus(Status.SENT);
            log.setErrorMessage(null);
            log.setTimestamp(Instant.now());
            logRepo.save(log);
            updateNotificationStatus(event.getId(), Status.SENT);
            publishStatusEvent(event, Status.SENT, NotificationLifecycleEventType.SENT, null);

            System.out.println("✅ SMS sent to " + event.getTo());

        } catch (Exception e) {
            log.setStatus(Status.FAILED);
            log.setErrorMessage(e.getMessage());
            log.setTimestamp(Instant.now());
            logRepo.save(log);
            updateNotificationStatus(event.getId(), Status.FAILED);
            publishStatusEvent(event, Status.FAILED, NotificationLifecycleEventType.FAILED, e.getMessage());

            System.err.println("❌ Failed to send SMS: " + e.getMessage());
        }
    }

    private void updateNotificationStatus(UUID notificationId, Status status) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(status);
            notificationRepository.save(notification);
        });
    }

    private TwilioConfig resolveTwilioConfig(NotificationEvent event) {
        if (event.getProviderAccountId() == null || event.getTenantId() == null) {
            return new TwilioConfig(
                    smsConfig.getAccountSid(),
                    smsConfig.getAuthToken(),
                    smsConfig.getFromNumber().toString()
            );
        }

        ProviderAccount provider = providerAccountRepository.findByIdAndTenantId(event.getProviderAccountId(), event.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Provider account not found for SMS notification"));

        try {
            Map<String, Object> config = objectMapper.readValue(provider.getConfigurationJson(), new TypeReference<>() {
            });
            return new TwilioConfig(
                    String.valueOf(config.get("accountSid")),
                    String.valueOf(config.get("authToken")),
                    String.valueOf(config.get("fromNumber"))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid Twilio provider configuration", ex);
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

    private record TwilioConfig(String accountSid, String authToken, String fromNumber) {
    }
}
