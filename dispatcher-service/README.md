# dispatcher-service

## Purpose

`dispatcher-service` is the internal Kafka router of NotiX. It listens to the main ingress topic and forwards notifications to the correct channel-specific topic based on the requested channel.

## Capabilities

- consumes from Kafka topic `notifications`
- inspects the `channel` field on `NotificationEvent`
- routes email events to `notifications.email`
- routes SMS events to `notifications.sms`
- keeps channel routing logic separate from API and sender services

## Main Runtime Flow

Input topic:

- `notifications`

Output topics:

- `notifications.email`
- `notifications.sms`

## Important Classes

- `NotificationRouter`
- `KafkaConfig`
- `NotificationController` for manual routing tests

## Demo Endpoint

- `POST /notifications/send`

This endpoint is mainly useful for manual testing of routing logic.

## Local Defaults

- Port: `7071`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `dispatcher-service/`.
