# AGENTS.md

## Repo purpose

NotiX is a multi-service notification delivery system built with Spring Boot and Kafka.
It accepts notification requests, persists metadata, routes by channel, sends via channel-specific workers, and retries failed deliveries before moving terminal failures to a dead-letter table.

This repo is the closest thing to "project training" for an assistant in local development: read this file first, then read the referenced module code when making changes.

## User-provided project context

The repo owner described this as the `Release 1.0` POC after roughly 21 days of iterative work.
That context is useful for intent and prioritization:

- The target story is an end-to-end notification platform with Kafka-based routing, retry, DLQ, observability, Swagger-based testing, and basic API protection.
- The current milestone is explicitly a proof of concept, not a finished SaaS product.
- The next-step framing from the owner is to stabilize a `release/1.0` branch and tag a `v1.0-POC` release once critical fixes are done.

Treat these notes as product context from the owner.
Prefer checked-in code over prose when they conflict.

## Stack

- Java 25 in module `pom.xml` files
- Spring Boot 3.5.13
- Apache Kafka
- PostgreSQL
- Prometheus + Grafana
- Twilio for SMS
- Spring Mail for email

## Service map

- `api-service`
  - Entry point for client requests
  - Exposes `POST /notifications/send` and `GET /notifications/status/{id}`
  - Saves a `Notification` row with `PENDING` status, then publishes to Kafka topic `notifications`
  - Local port: `7070`

- `dispatcher-service`
  - Consumes from `notifications`
  - Routes by `Channel`
  - Publishes to `notifications.email` or `notifications.sms`
  - Local port: `7071`

- `email-sender-service`
  - Consumes from `notifications.email`
  - Persists a `DeliveryLog`
  - Current implementation is effectively a mocked email send with delivery logging
  - Local port: `7072`

- `sms-sender-service`
  - Consumes from `notifications.sms`
  - Sends with Twilio and persists a `DeliveryLog`
  - Local port: `7073`

- `retry-scheduler-service`
  - Scans failed delivery logs on a fixed schedule
  - Republishes retryable notifications to `notifications`
  - Persists dead-letter rows after max attempts
  - Local port: `7074`

- `common`
  - Shared DTOs and enums used across services
  - The repo now has a root reactor `pom.xml`, so the full project can be built together from the repo root

## Main flow

1. `api-service` accepts a `NotificationEvent` at `/notifications/send`.
2. `NotificationService` generates a UUID, saves notification metadata, and publishes to Kafka topic `notifications`.
3. `dispatcher-service` listens on `notifications` and routes by `event.getChannel()`.
4. Sender services consume their channel-specific topics and persist `DeliveryLog` records.
5. `retry-scheduler-service` retries failed deliveries up to `MAX_ATTEMPTS = 3`.
6. Final failures are copied into the dead-letter table/entity.

## Shared contracts

- `common/src/main/java/com/abhishek/notix/common/dto/NotificationEvent.java`
  - Canonical message sent through Kafka
  - Fields: `id`, `to`, `channel`, `template`, `params`

- `common/src/main/java/com/abhishek/notix/common/dto/SendRequest.java`
  - Public request DTO used by `api-service`
  - Keeps transport input separate from the internal Kafka event contract

- `common/src/main/java/com/abhishek/notix/common/enums/Channel.java`
  - `EMAIL`, `SMS`

- `common/src/main/java/com/abhishek/notix/common/enums/Status.java`
  - `PENDING`, `SENT`, `FAILED`

## Infra defaults

- Kafka: `localhost:9092`
- Postgres: `localhost:5433`, database `notix`, user `notix_user`, password `notix_pass`
- Prometheus: `localhost:9090`
- Grafana: `localhost:3000`

## Security and API behavior

- API key header: `X-API-KEY`
- Default local API key: `notix-secret-key`
- Swagger UI: `http://localhost:7070/swagger-ui.html`
- Rate limiting is implemented in `api-service` with Bucket4j and keyed by API key

## Project status snapshot

Based on the checked-in code plus the owner's notes, the intended v1.0 POC scope is:

- Five services:
  - `api-service`
  - `dispatcher-service`
  - `email-sender-service`
  - `sms-sender-service`
  - `retry-scheduler-service`
- Async delivery flow:
  - `notifications` -> `notifications.email` or `notifications.sms`
- Persistence:
  - notification metadata
  - delivery logs
  - dead-letter records
- Observability:
  - actuator
  - Prometheus
  - Grafana
- Basic security:
  - API key authentication
  - rate limiting

The owner also called out learning and design themes around distributed systems, SaaS readiness, circuit breakers, and multi-tenancy.
Some of those themes are not yet visible in the current codebase and should be treated as roadmap or background context unless confirmed in files.

## Build and run

There is now a root reactor `pom.xml`, so the repo can be built as one Maven graph.

Recommended local sequence:

1. Start infra from `infrastructure/docker/docker-compose.yml`
2. Build from the root reactor or install `common` if working on an individual service in isolation
3. Run each service independently

Useful commands:

```bash
./api-service/mvnw -f pom.xml package -DskipTests
docker compose -f infrastructure/docker/docker-compose.yml up -d
./common/mvnw -f common/pom.xml install
./api-service/mvnw -f api-service/pom.xml spring-boot:run
./dispatcher-service/mvnw -f dispatcher-service/pom.xml spring-boot:run
./email-sender-service/mvnw -f email-sender-service/pom.xml spring-boot:run
./sms-sender-service/mvnw -f sms-sender-service/pom.xml spring-boot:run
./retry-scheduler-service/mvnw -f retry-scheduler-service/pom.xml spring-boot:run
```

## High-signal gotchas

- The project now targets JDK 25; local builds will fail until the active `JAVA_HOME` / `java -version` is also 25.
- Runtime config is split between `application.properties` and `application.yml`.
  - Ports are defined in `application.properties`
  - Most Kafka, datasource, and actuator config is in `application.yml`
- `SendRequest.to` is intentionally channel-neutral so the same request DTO can support both email and SMS.
- Keep DTOs and enums in `common`, but keep JPA entities local to each service unless you explicitly want schema coupling.
- Kafka producer and consumer beans are defined explicitly in Java config; when changing event shape or serialization, update every affected service, not just YAML.
- `email-sender-service` contains both a listener component and a `@KafkaListener` on the service method; be careful not to introduce duplicate-consumption behavior when refactoring.
- The owner mentioned tenant foundations, circuit breakers, and Swagger for all services, but a quick scan currently shows:
  - no `tenantId` fields in the checked-in Java code
  - no Hystrix or Resilience4j usage in the checked-in Java code
  - Springdoc wiring is clearly present in `api-service`; other services may still be partial, planned, or documented separately

## Where to look first for common tasks

- API request handling:
  - `api-service/src/main/java/com/abhishek/notix/api_service/controller/NotificationController.java`
  - `api-service/src/main/java/com/abhishek/notix/api_service/service/NotificationService.java`

- Kafka routing:
  - `dispatcher-service/src/main/java/com/abhishek/notix/dispatcher_service/service/NotificationRouter.java`

- Email send path:
  - `email-sender-service/src/main/java/com/abhishek/notix/email_sender_service/service/EmailNotificationListener.java`
  - `email-sender-service/src/main/java/com/abhishek/notix/email_sender_service/service/EmailSenderService.java`

- SMS send path:
  - `sms-sender-service/src/main/java/com/abhishek/notix/sms_sender_service/service/SmsNotificationListener.java`
  - `sms-sender-service/src/main/java/com/abhishek/notix/sms_sender_service/service/SmsSenderService.java`

- Retry and DLQ behavior:
  - `retry-scheduler-service/src/main/java/com/abhishek/notix/retry_scheduler_service/service/RetrySchedulerService.java`

## Working guidance for future changes

- Keep shared message shape changes in `common` and then verify every producer and consumer.
- Preserve topic names unless a migration touches all services together.
- When fixing delivery-state issues, inspect both `Notification` and `DeliveryLog` entities and the retry scheduler logic.
- Prefer small, module-local changes unless the task clearly spans service boundaries.
