# api-service

## Purpose

`api-service` is the public entrypoint into NotiX. It accepts notification requests from clients, persists the initial notification record, publishes the internal event to Kafka, and exposes status lookup APIs.

## Capabilities

- accepts new notification requests over HTTP
- validates request payloads using `SendRequest`
- protects endpoints with API key auth
- applies in-memory rate limiting
- stores notification metadata in PostgreSQL
- publishes `NotificationEvent` to Kafka topic `notifications`
- returns delivery status and attempt history

## Main Endpoints

- `POST /notifications/send`
- `GET /notifications/status/{id}`

Test/demo endpoints:

- `POST /test/api/send`
- `GET /test/api/rate-limiter`

## Inputs and Outputs

### Input

- HTTP request from external clients

### Output

- Kafka event to topic `notifications`
- JSON status response to clients

## Data It Owns

Tables used by this service:

- `notifications`
- `delivery_logs` for status lookup

## Important Classes

- `NotificationController`
- `NotificationService`
- `KafkaConfig`
- `SecurityConfig`
- `ApiKeyAuthFilter`
- `RateLimitingFilter`

## Local Defaults

- Port: `7070`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`
- API key: `notix-secret-key`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `api-service/`.
