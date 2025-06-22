package com.abhishek.notix.api_service.service;

import com.abhishek.notix.api_service.model.Notification;
import com.abhishek.notix.api_service.repo.NotificationRepository;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${kafka.producer.topic}")
    private String topic;

    public UUID processNotification(NotificationEvent event) {
        UUID notificationId = UUID.randomUUID();
        event.setId(notificationId);

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
}
