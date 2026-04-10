# retry-scheduler-service

`retry-scheduler-service` is the resilience worker for NotiX. It decides when failed deliveries should be retried and when they should be moved to the dead-letter store.

## Responsibilities

- scan failed `delivery_logs`
- republish retryable work to `notifications`
- create the next `PENDING` attempt record before republishing
- mark terminal failures and write `dead_letters`
- emit `NotificationStatusEvent` for dead-lettered outcomes
- expose operational endpoints for retry and DLQ inspection

## Runtime Cadence

- retry scan: every `15s`
- DLQ sweep: every `60s`

## Topic Flow

- produces: `notifications`
- produces: `notifications.status`

## Data Access

- reads `notifications`
- reads `delivery_logs`
- writes `delivery_logs`
- updates `notifications`
- writes `dead_letters`

## Endpoints

- `POST /retry/trigger`
- `GET /retry/dead-letters`
- `GET /dlq`
- `GET /dlq/channel/{channel}`
- `GET /dlq/template/{template}`
- `GET /dlq/search`
- `POST /test/retry/trigger`

## Important Classes

- `RetrySchedulerService`
- `RetryController`
- `DeadLetterController`
- `DeliveryLogRepository`
- `NotificationRepository`
- `DeadLetterRepository`

## Local Defaults

- Port: `7074`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `retry-scheduler-service/`.

## Read Next

- [Root README](../README.md)
- [Low-Level Design](../docs/LLD.md)
- [Database Design](../docs/Database-Design.md)
