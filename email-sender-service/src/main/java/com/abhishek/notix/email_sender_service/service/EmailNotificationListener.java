package com.abhishek.notix.email_sender_service.service;

import com.abhishek.notix.common.dto.NotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationListener {

    private final EmailSenderService emailSenderService;

    public EmailNotificationListener(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    @KafkaListener(topics = "${kafka.consumer.email-topic}", groupId = "email-sender-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(NotificationEvent event) {
        System.out.println("📥 Received Email Event: " + event);
        emailSenderService.sendEmail(event);
    }
}
