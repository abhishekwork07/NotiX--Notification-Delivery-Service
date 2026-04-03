package com.abhishek.notix.api_service.service;

import com.abhishek.notix.api_service.dto.StatusResponseDTO;
import com.abhishek.notix.api_service.model.DeliveryLog;
import com.abhishek.notix.api_service.model.Notification;
import com.abhishek.notix.api_service.repo.DeliveryLogRepository;
import com.abhishek.notix.api_service.repo.NotificationRepository;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.dto.SendRequest;
import com.abhishek.notix.common.enums.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Value("${kafka.producer.topic}")
    private String topic;

    public UUID processNotification(SendRequest request) {
        NotificationEvent event = new NotificationEvent(
                null,
                request.getTo(),
                request.getChannel(),
                request.getTemplate(),
                request.getParams(),
                1
        );
        return processNotification(event);
    }

    public UUID processNotification(NotificationEvent event) {
        UUID notificationId = UUID.randomUUID();
        event.setId(notificationId);
        event.setAttemptNo(1);

        // 1. Save notification metadata in DB
        Notification entity = new Notification();
        entity.setId(notificationId);
        entity.setRecipient(event.getTo());
        entity.setChannel(event.getChannel());
        entity.setTemplate(event.getTemplate());
        entity.setStatus(Status.PENDING);
        notificationRepository.save(entity);

        // 2. Send notification to a Kafka topic
        kafkaTemplate.send(topic, notificationId.toString(), event);

        return notificationId;
    }

    public Optional<StatusResponseDTO> getStatus(UUID id) {
        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        if (notificationOpt.isEmpty()) return Optional.empty();

        Notification notification = notificationOpt.get();
        List<DeliveryLog> attempts = deliveryLogRepository.findByNotificationIdOrderByAttemptNoAscTimestampAsc(id);

        return Optional.of(new StatusResponseDTO(notification, attempts));
    }

}
