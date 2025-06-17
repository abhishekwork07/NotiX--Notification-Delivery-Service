package com.abhishek.notix.dispatcher_service.controller;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${kafka.producer.email-topic}")
    private String emailTopic;

    @Value("${kafka.producer.sms-topic}")
    private String smsTopic;

    public NotificationController(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody Map<String, Object> request) {
        try {
            String to = (String) request.get("to");
            String channelStr = ((String) request.get("channel")).toUpperCase();
            String template = (String) request.get("template");
            Map<String, Object> params = (Map<String, Object>) request.get("params");

            Channel channel = Channel.valueOf(channelStr);
            NotificationEvent event = new NotificationEvent(
                    UUID.randomUUID(), to, channel, template, params
            );

            String topic = (channel == Channel.EMAIL) ? emailTopic : smsTopic;
            kafkaTemplate.send(topic, event.getId().toString(), event);

            return ResponseEntity.ok("Notification dispatched to Kafka topic: " + topic);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid request or internal error: " + e.getMessage());
        }
    }
}
