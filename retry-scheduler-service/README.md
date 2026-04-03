# retry-scheduler-service

## Purpose

`retry-scheduler-service` is responsible for resilience in NotiX. It identifies failed delivery attempts, creates the next retry attempt, republishes the notification to Kafka, and moves terminal failures into the dead-letter store.

## Capabilities

- scans for retryable failed delivery attempts
- republishes notifications with incremented `attemptNo`
- creates `PENDING` retry logs before republishes
- marks terminal failures after max attempts
- stores dead-letter records for operational inspection
- exposes manual retry and DLQ inspection endpoints

## Retry Model

- max attempts: `3`
- retry scan interval: `15 seconds`
- DLQ sweep interval: `60 seconds`

## Kafka Usage

Produces to:

- `notifications`

That means retries re-enter the same normal routing pipeline as first-time notifications.

## Main Endpoints

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
- `DeadLetterRepository`

## Data It Updates

- `delivery_logs`
- `notifications`
- `dead_letters`

## Local Defaults

- Port: `7074`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `retry-scheduler-service/`.
