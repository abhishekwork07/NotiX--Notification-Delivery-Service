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

    @KafkaListener(topics = "${kafka.consumer.topics}", groupId = "${spring.application.name}-group")
    public void route(NotificationEvent event) {
        String targetTopic = Channel.EMAIL.equals(event.getChannel()) ? emailTopic : smsTopic;
        kafkaTemplate.send(targetTopic, String.valueOf(event.getId()), event);
    }
}
