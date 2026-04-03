# email-sender-service

## Purpose

`email-sender-service` is the email delivery worker in NotiX. It consumes email notifications from Kafka, performs the email send flow, stores delivery attempt history, and updates the overall notification status.

## Capabilities

- consumes email notifications from Kafka
- processes `NotificationEvent` for channel `EMAIL`
- records attempt-level delivery logs
- updates the notification status to `SENT` or `FAILED`
- supports idempotent handling by `(notificationId, attemptNo)`

## Topics

Consumes:

- `notifications.email`

## Current Implementation Note

The mail send logic is currently mocked through the configured `JavaMailSender` setup. This service already exercises the delivery-state flow even though SMTP integration is not fully implemented.

## Important Classes

- `EmailNotificationListener`
- `EmailSenderService`
- `KafkaConfig`
- `MailConfig`

## Demo Endpoint

- `POST /test/email/send`

## Data It Updates

- `delivery_logs`
- `notifications`

## Local Defaults

- Port: `7072`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `email-sender-service/`.
