# sms-sender-service

## Purpose

`sms-sender-service` is the SMS delivery worker in NotiX. It consumes SMS notifications from Kafka, invokes Twilio for delivery, stores attempt logs, and updates the notification status.

## Capabilities

- consumes SMS notifications from Kafka
- processes `NotificationEvent` for channel `SMS`
- sends SMS using Twilio
- records each attempt in `delivery_logs`
- updates the notification status to `SENT` or `FAILED`
- prevents duplicate final processing for the same `(notificationId, attemptNo)`

## Topics

Consumes:

- `notifications.sms`

## Important Classes

- `SmsNotificationListener`
- `SmsSenderService`
- `SmsConfig`
- `KafkaConfig`

## Demo Endpoint

- `POST /test/sms/send`

## Required External Config

Set valid Twilio values in configuration before testing real SMS delivery:

- `twilio.account-sid`
- `twilio.auth-token`
- `twilio.from-number`

## Data It Updates

- `delivery_logs`
- `notifications`

## Local Defaults

- Port: `7073`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `sms-sender-service/`.
