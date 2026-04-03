# NotiX Low-Level Design

## 1. Scope

This document maps the current codebase structure to the actual runtime behavior of NotiX. It focuses on:

- package/module responsibilities
- main classes and their roles
- endpoint contracts
- Kafka topics and listeners
- persistence objects
- retry and DLQ logic
- configuration and runtime defaults

## 2. Module Breakdown

## `common`

Purpose:

- shared DTOs
- shared enums

Key classes:

- `com.abhishek.notix.common.dto.SendRequest`
- `com.abhishek.notix.common.dto.NotificationEvent`
- `com.abhishek.notix.common.dto.StatusResponse`
- `com.abhishek.notix.common.enums.Channel`
- `com.abhishek.notix.common.enums.Status`

### `SendRequest`

Used by public API input.

Fields:

- `to`
- `channel`
- `template`
- `params`

### `NotificationEvent`

Used internally across Kafka-based services.

Fields:

- `id`
- `to`
- `channel`
- `template`
- `params`
- `attemptNo`

`attemptNo` is important for retry-aware delivery logging.

## `api-service`

### Responsibilities

- accept client requests
- store notification metadata
- publish notification events
- expose notification status
- enforce API auth and rate limits

### Main classes

#### `NotificationController`

Path prefix:

- `/notifications`

Endpoints:

- `POST /send`
- `GET /status/{id}`

#### `NotificationService`

Key methods:

- `processNotification(SendRequest request)`
- `processNotification(NotificationEvent event)`
- `getStatus(UUID id)`

Behavior:

1. creates a `NotificationEvent`
2. generates a new UUID
3. persists `Notification` with `PENDING`
4. publishes to Kafka topic `notifications`
5. returns the generated notification ID

#### `KafkaConfig`

Configures:

- `ProducerFactory<String, NotificationEvent>`
- `KafkaTemplate<String, NotificationEvent>`

Serialization:

- key: `StringSerializer`
- value: `JsonSerializer`

#### `ApiKeyAuthFilter`

Checks:

- header: `X-API-KEY`

Rejects requests with:

- `401 Unauthorized`

#### `RateLimitingFilter`

Implements:

- in-memory per-key bucket
- limit of `10 requests / minute`

Rejects excess requests with:

- `429 Too Many Requests`

#### `SecurityConfig`

Registers filters for:

- `/notifications/*`
- `/test/*`

### Persistence

#### `Notification`

Table:

- `notifications`

Fields:

- `id`
- `recipient`
- `channel`
- `template`
- `status`
- `createdAt`
- `updatedAt`

#### `DeliveryLog`

Table:

- `delivery_logs`

Fields:

- `id`
- `notificationId`
- `attemptNo`
- `status`
- `errorMessage`
- `timestamp`

#### Repositories

- `NotificationRepository`
- `DeliveryLogRepository`

Special query method:

- `findByNotificationIdOrderByAttemptNoAscTimestampAsc`

### Response DTO

`StatusResponseDTO` returns:

- `id`
- `status`
- `attempts[]`

Each attempt contains:

- `attemptNo`
- `status`
- `timestamp`

## `dispatcher-service`

### Responsibilities

- consume from the main topic
- route to downstream channel topics

### Main classes

#### `NotificationRouter`

Kafka listener:

- consumes from `notifications`

Routing logic:

- `EMAIL -> notifications.email`
- `SMS -> notifications.sms`

#### `KafkaConfig`

Configures:

- Kafka consumer factory for `NotificationEvent`
- Kafka producer factory for routed channel events
- listener container factory

## `email-sender-service`

### Responsibilities

- consume email events
- execute email send flow
- persist attempt logs
- update notification status

### Main classes

#### `EmailNotificationListener`

Kafka listener:

- consumes from `${kafka.consumer.email-topic}`

Delegates to:

- `EmailSenderService.sendEmail`

#### `EmailSenderService`

Behavior:

1. checks whether the `(notificationId, attemptNo)` log already exists in a final state
2. creates or reuses a `PENDING` delivery log
3. performs the email send flow
4. updates delivery log to `SENT` or `FAILED`
5. updates the notification status

Current implementation note:

- the actual mail send is mocked in code comments
- delivery tracking is real

#### `MailConfig`

Provides:

- `JavaMailSender`

Current implementation:

- `JavaMailSenderImpl` mock-style bean

### Persistence

Local entity classes:

- `Notification`
- `DeliveryLog`

Repositories:

- `NotificationRepository`
- `DeliveryLogRepository`

Important query methods:

- `existsByNotificationIdAndAttemptNo`
- `findByNotificationIdAndAttemptNo`

## `sms-sender-service`

### Responsibilities

- consume SMS events
- call Twilio
- persist attempt logs
- update notification status

### Main classes

#### `SmsNotificationListener`

Kafka listener:

- consumes from `${kafka.consumer.sms-topic}`

Delegates to:

- `SmsSenderService.sendSms`

#### `SmsSenderService`

Behavior:

1. checks whether the `(notificationId, attemptNo)` log is already final
2. creates or reuses a `PENDING` delivery log
3. sends SMS via Twilio
4. updates delivery log to `SENT` or `FAILED`
5. updates the notification status

#### `SmsConfig`

Loads:

- Twilio account SID
- auth token
- from number

Runs:

- `Twilio.init(...)` in `@PostConstruct`

### Persistence

Local entity classes:

- `Notification`
- `DeliveryLog`

Repositories:

- `NotificationRepository`
- `DeliveryLogRepository`

## `retry-scheduler-service`

### Responsibilities

- find retryable failed attempts
- schedule retries
- republish with incremented attempt number
- identify terminal failures
- store dead-letter entries

### Main classes

#### `RetrySchedulerService`

#### `retryFailedMessages()`

Annotation:

- `@Scheduled(fixedDelay = 15000)`

Logic:

1. fetch latest failed attempts with `attemptNo < MAX_ATTEMPTS`
2. fetch corresponding `Notification`
3. compute `nextAttempt`
4. skip if that attempt number already exists
5. insert a `PENDING` retry log
6. republish a new `NotificationEvent`
7. on publish failure, mark retry log `FAILED`
8. if max attempts reached, persist to DLQ

#### `moveFailedLogsToDLQ()`

Annotation:

- `@Scheduled(fixedDelay = 60000)`

Logic:

1. fetch terminal failures
2. load corresponding notification
3. set notification status to `FAILED`
4. persist dead-letter record

#### `buildNotificationEvent(Notification, attemptNo)`

Builds a retry event using:

- same notification ID
- same recipient
- same channel
- same template
- incremented `attemptNo`

#### `RetryController`

Endpoints:

- `POST /retry/trigger`
- `GET /retry/dead-letters`

#### `DeadLetterController`

Endpoints:

- `GET /dlq`
- `GET /dlq/channel/{channel}`
- `GET /dlq/template/{template}`
- `GET /dlq/search`

### Persistence

Local entity classes:

- `Notification`
- `DeliveryLog`
- `DeadLetter`

Repositories:

- `NotificationRepository`
- `DeliveryLogRepository`
- `DeadLetterRepository`

#### Important retry queries

`findLatestFailedAttempts(maxRetries)`

- only latest failed attempt per notification
- only attempts below retry limit

`findTerminalFailures(maxRetries)`

- only latest failed attempt per notification
- only attempts at or beyond retry limit

## 3. Configuration Model

## Shared local defaults

- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`
- DB name: `notix`
- DB user: `notix_user`
- DB password: `notix_pass`

## Service ports

- `api-service`: `7070`
- `dispatcher-service`: `7071`
- `email-sender-service`: `7072`
- `sms-sender-service`: `7073`
- `retry-scheduler-service`: `7074`

## Kafka topics

- ingress: `notifications`
- email: `notifications.email`
- sms: `notifications.sms`

## Security config

Default API key:

- `notix-secret-key`

## 4. Build Model

The repo now includes a root Maven reactor:

- root `pom.xml`

This allows:

- full multi-module build from repo root
- correct build ordering for `common`

### Full build

```bash
mvn -f pom.xml package -DskipTests
```

### Install `common` for external local usage

```bash
./common/mvnw -f common/pom.xml install -DskipTests
```

## 5. Runtime Infrastructure

Defined in:

- `infrastructure/docker/docker-compose.yml`

Containers:

- Zookeeper
- Kafka
- PostgreSQL
- Prometheus
- Grafana

## 6. Design Decisions

### Shared DTOs, local entities

Chosen because:

- contract reuse is high-value
- JPA schema ownership should remain service-local
- it avoids tighter database coupling than necessary

### Kafka as the async backbone

Chosen because:

- producers and consumers remain decoupled
- retry can reuse the same ingress path
- new channel services can be added later

### Delivery logs per attempt

Chosen because:

- retry history becomes visible
- the status API can show progression over time
- operational debugging becomes easier

## 7. Current Limitations

- email delivery path is mocked rather than fully integrated
- one shared PostgreSQL instance is used in local development
- there is no advanced backoff strategy yet
- tests are limited
- auth and tenancy are still POC-grade

## 8. Recommended Next Engineering Steps

- add integration tests for end-to-end notification flow
- externalize secrets for Twilio and API keys
- add migration tooling instead of relying only on `ddl-auto=update`
- improve retry policy with exponential backoff and jitter
- document or automate topic creation
- introduce provider abstraction for email and SMS
