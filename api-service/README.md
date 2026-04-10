# api-service

`api-service` is the public edge of NotiX. It carries both the original v1 notification API and the newer v2 SaaS-oriented control plane.

## Responsibilities

- accept v1 and v2 notification requests
- persist canonical `notifications` rows
- publish `NotificationEvent` to Kafka
- expose notification status and attempt history
- manage tenants, memberships, API keys, providers, templates, schedules, usage, and webhooks
- authenticate requests using API keys, headers, or bearer JWTs
- consume `NotificationStatusEvent` and translate it into usage and webhook work

## Main APIs

### v1

- `POST /notifications/send`
- `GET /notifications/status/{id}`

### Login

- `POST /auth/login`
- `POST /v2/auth/login`

### v2

- `POST /v2/tenants`
- `POST /v2/tenant-memberships`
- `POST /v2/api-keys`
- `POST /v2/providers`
- `POST /v2/templates`
- `POST /v2/webhooks`
- `POST /v2/notifications`
- `GET /v2/notifications/{id}`
- `GET /v2/notifications/{id}/attempts`
- `POST /v2/schedules`
- `GET /v2/usage`

## Authentication Modes

- `X-API-KEY` for v1 APIs
- `X-NOTIX-BOOTSTRAP-KEY` for tenant bootstrap
- `X-NOTIX-API-KEY` for tenant machine access
- `Authorization: Bearer <jwt>` for local application login
- `X-NOTIX-EXTERNAL-USER-ID` plus `X-NOTIX-TENANT-ID` for external-user-header mode

## Runtime Responsibilities

### Intake And Orchestration

- creates `notifications` rows
- handles idempotency for v2 notification creation
- publishes to the `notifications` Kafka topic

### Control Plane

- owns `tenants`, `platform_users`, `tenant_memberships`
- creates `api_keys`, `provider_accounts`, `notification_templates`, `webhook_endpoints`
- records `audit_logs`

### Runtime Support

- consumes `notifications.status`
- writes `usage_events`
- creates and dispatches `webhook_deliveries`
- dispatches due `notification_schedules`
- hosts Eureka Server at `http://localhost:7070/eureka-dashboard`
- exposes infrastructure health at `GET /monitoring/infra/health`

## Scheduled Jobs

- `dispatchDueSchedules()` every `5s`
- `dispatchWebhookDeliveries()` every `10s`

## Important Classes

- `NotificationController`
- `NotificationService`
- `LoginController`
- `LocalJwtAuthService`
- `NotixV2Controller`
- `NotixV2Service`
- `NotificationStatusListener`
- `ApiKeyAuthFilter`
- `RateLimitingFilter`
- `V2AuthFilter`
- `DefaultIdentitySeeder`

## Tables Touched

- `notifications`
- `delivery_logs`
- `tenants`
- `platform_users`
- `tenant_memberships`
- `api_keys`
- `provider_accounts`
- `notification_templates`
- `notification_schedules`
- `usage_events`
- `webhook_endpoints`
- `webhook_deliveries`
- `audit_logs`

## Local Defaults

- Port: `7070`
- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5433`
- Swagger UI: `http://localhost:7070/swagger-ui.html`
- Eureka dashboard: `http://localhost:7070/eureka-dashboard`
- Infrastructure health: `http://localhost:7070/monitoring/infra/health`
- v1 API key: `notix-secret-key`
- v2 bootstrap key: `notix-bootstrap-admin-key`
- admin login: `admin / admin123`
- operator login: `operator / operator123`

## Run

```bash
./mvnw -f pom.xml spring-boot:run
```

Run from inside `api-service/`.

## Read Next

- [Root README](../README.md)
- [High-Level Design](../docs/HLD.md)
- [Low-Level Design](../docs/LLD.md)
- [Database Design](../docs/Database-Design.md)
