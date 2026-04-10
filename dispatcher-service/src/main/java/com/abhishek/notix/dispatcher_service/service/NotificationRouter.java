package com.abhishek.notix.dispatcher_service.service;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
public class NotificationRouter {
    public final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    @Value("${kafka.producer.email-topic}") private String emailTopic;
    @Value("${kafka.producer.sms-topic}") private String smsTopic;

    public NotificationRouter(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "notifications", groupId = "dispatcher-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleNotification(NotificationEvent event) {
        System.out.println("📩 Received event: " + event);

        if (event.getChannel() == null) {
            System.err.println("⚠️ Channel is missing in event: " + event);
            return;
        }

        switch (event.getChannel()) {
            case EMAIL -> {
                System.out.println("📬 Routing to Email Topic");
                kafkaTemplate.send(emailTopic, event.getId().toString(), event);
            }
            case SMS -> {
                System.out.println("📲 Routing to SMS Topic");
                kafkaTemplate.send(smsTopic, event.getId().toString(), event);
            }
            default -> System.err.println("❌ Unknown channel: " + event.getChannel());
        }
    }
}
