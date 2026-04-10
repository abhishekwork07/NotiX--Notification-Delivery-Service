package com.abhishek.notix.api_service.v2.service;

import com.abhishek.notix.common.dto.NotificationStatusEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationStatusListener {

    private final NotixV2Service notixV2Service;

    public NotificationStatusListener(NotixV2Service notixV2Service) {
        this.notixV2Service = notixV2Service;
    }

    @KafkaListener(topics = "${kafka.consumer.status-topic}", groupId = "api-status-group",
            containerFactory = "notificationStatusKafkaListenerContainerFactory")
    public void consume(NotificationStatusEvent event) {
        notixV2Service.handleStatusEvent(event);
    }
}
