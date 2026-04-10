# sms-sender-service

`sms-sender-service` is the SMS delivery worker in NotiX.

## Responsibilities

- consume SMS work from Kafka
- resolve the tenant-selected SMS provider account
- send the message through Twilio or future provider integrations
- persist attempt history in `delivery_logs`
- update the canonical `notifications` row
- emit `NotificationStatusEvent` to `notifications.status`

## Topic Flow

- consumes: `notifications.sms`
- produces: `notifications.status`

## Data Access

- reads `notifications`
- reads `provider_accounts`
- writes `delivery_logs`
- updates `notifications`

## Important Classes

- `SmsNotificationListener`
- `SmsSenderService`
- `KafkaConfig`
- `SmsConfig`
- `NotificationTestController`

## Implementation Notes

- duplicate final handling is prevented per `(notificationId, attemptNo)`
- the service is tenant-aware through `tenant_id` and provider account resolution
- valid Twilio settings are required for real SMS delivery

## Required External Configuration

- `twilio.account-sid`
- `twilio.auth-token`
- `twilio.from-number`

## Local Defaults

- Port: `7073`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `sms-sender-service/`.

## Read Next

- [Root README](../README.md)
- [Low-Level Design](../docs/LLD.md)
- [Database Design](../docs/Database-Design.md)
