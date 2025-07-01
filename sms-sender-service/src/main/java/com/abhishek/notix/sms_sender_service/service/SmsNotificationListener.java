package com.abhishek.notix.sms_sender_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationListener {

    private final SmsSenderService smsSenderService;

    public SmsNotificationListener(SmsSenderService smsSenderService) {
        this.smsSenderService = smsSenderService;
    }

    @KafkaListener(topics = "${kafka.consumer.sms-topic}", groupId = "sms-sender-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(NotificationEvent event) {
        System.out.println("📥 Received SMS Event: " + event);
        smsSenderService.sendSms(event);
    }
}

