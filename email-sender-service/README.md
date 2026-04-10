# email-sender-service

`email-sender-service` is the email delivery worker in NotiX.

## Responsibilities

- consume email work from Kafka
- resolve the preferred provider configuration for the tenant
- perform the email send flow
- persist or update the current attempt in `delivery_logs`
- update the canonical `notifications` row
- emit `NotificationStatusEvent` back to `api-service`

## Topic Flow

- consumes: `notifications.email`
- produces: `notifications.status`

## Data Access

- reads `notifications`
- reads `provider_accounts`
- writes `delivery_logs`
- updates `notifications`

## Important Classes

- `EmailNotificationListener`
- `EmailSenderService`
- `KafkaConfig`
- `MailConfig`
- `NotificationTestController`

## Implementation Notes

- the service is idempotent by notification ID and attempt number
- it participates in the same shared `notifications` and `delivery_logs` schema used by the rest of the repo
- provider configuration is tenant-aware even though SMTP behavior is still simple in the current implementation

## Local Defaults

- Port: `7072`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `email-sender-service/`.

## Read Next

- [Root README](../README.md)
- [Low-Level Design](../docs/LLD.md)
- [Database Design](../docs/Database-Design.md)
