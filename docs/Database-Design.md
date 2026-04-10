# NotiX Database Design

## 1. Purpose

This document explains the current NotiX database model, the intent behind the main tables, and the relationships that support the v2 SaaS-ready architecture. The schema is currently stored in one shared PostgreSQL database for local development, but the model is already organized around tenant-aware boundaries.

## 2. Design Principles

- keep notification intent separate from delivery attempts
- make `tenant_id` the core scoping mechanism for SaaS data
- let control-plane tables live alongside data-plane tables
- keep JPA entities local to each service even when table names overlap
- treat usage, webhook, and audit data as operational records, not as delivery-state substitutes

## 3. ERD

The diagram below shows the logical relationships used by the current implementation. Several of these are application-level UUID relationships rather than explicit JPA associations.

```mermaid
erDiagram
    TENANTS {
        uuid id PK
        string name
        string domain UK
        timestamp created_at
    }

    PLATFORM_USERS {
        uuid id PK
        string external_user_id UK
        string email
        string username UK
        string display_name
        string auth_provider
        string password_hash
        boolean is_platform_admin
        boolean active
        timestamp created_at
    }

    TENANT_MEMBERSHIPS {
        uuid id PK
        uuid tenant_id FK
        uuid platform_user_id FK
        string role
        boolean active
        timestamp created_at
    }

    API_KEYS {
        uuid id PK
        uuid tenant_id FK
        string name
        string key_prefix
        string hashed_key UK
        uuid created_by_user_id FK
        boolean active
        timestamp last_used_at
        timestamp created_at
    }

    PROVIDER_ACCOUNTS {
        uuid id PK
        uuid tenant_id FK
        string name
        string channel
        string provider_type
        text configuration_json
        boolean active
        timestamp created_at
    }

    NOTIFICATION_TEMPLATES {
        uuid id PK
        uuid tenant_id FK
        string name
        string channel
        string subject_template
        text body_template
        boolean active
        timestamp created_at
    }

    NOTIFICATIONS {
        uuid id PK
        uuid tenant_id FK
        string recipient
        string channel
        string template
        uuid template_id FK
        uuid provider_account_id FK
        string idempotency_key
        string subject
        text body
        timestamp scheduled_at
        string requested_by_type
        string requested_by_id
        string status
        timestamp created_at
        timestamp updated_at
    }

    DELIVERY_LOGS {
        bigint id PK
        uuid notification_id FK
        uuid tenant_id FK
        string channel
        uuid provider_account_id FK
        int attempt_no
        string status
        string error_message
        timestamp timestamp
    }

    NOTIFICATION_SCHEDULES {
        uuid id PK
        uuid tenant_id FK
        uuid notification_id FK
        timestamp scheduled_at
        string status
        string last_error
        timestamp dispatched_at
        timestamp created_at
    }

    USAGE_EVENTS {
        uuid id PK
        uuid tenant_id FK
        uuid notification_id FK
        string channel
        string event_type
        int quantity
        text metadata_json
        timestamp created_at
    }

    WEBHOOK_ENDPOINTS {
        uuid id PK
        uuid tenant_id FK
        string name
        string url
        string secret
        string subscribed_events
        boolean active
        timestamp created_at
    }

    WEBHOOK_DELIVERIES {
        uuid id PK
        uuid tenant_id FK
        uuid endpoint_id FK
        uuid notification_id FK
        string event_type
        text payload_json
        string status
        int attempts
        timestamp next_attempt_at
        string last_error
        timestamp delivered_at
        timestamp created_at
    }

    AUDIT_LOGS {
        uuid id PK
        uuid tenant_id FK
        string actor_type
        string actor_id
        string action
        string resource_type
        string resource_id
        text metadata_json
        timestamp created_at
    }

    DEAD_LETTERS {
        uuid id PK
        uuid tenant_id FK
        string recipient
        string channel
        string template
        string status
        string error_message
        timestamp created_at
    }

    TENANTS ||--o{ TENANT_MEMBERSHIPS : scopes
    PLATFORM_USERS ||--o{ TENANT_MEMBERSHIPS : joins
    TENANTS ||--o{ API_KEYS : owns
    PLATFORM_USERS ||--o{ API_KEYS : creates
    TENANTS ||--o{ PROVIDER_ACCOUNTS : configures
    TENANTS ||--o{ NOTIFICATION_TEMPLATES : owns
    TENANTS ||--o{ NOTIFICATIONS : submits
    NOTIFICATION_TEMPLATES ||--o{ NOTIFICATIONS : renders
    PROVIDER_ACCOUNTS ||--o{ NOTIFICATIONS : preferred
    TENANTS ||--o{ DELIVERY_LOGS : scopes
    NOTIFICATIONS ||--o{ DELIVERY_LOGS : attempts
    PROVIDER_ACCOUNTS ||--o{ DELIVERY_LOGS : handles
    TENANTS ||--o{ NOTIFICATION_SCHEDULES : owns
    NOTIFICATIONS ||--o| NOTIFICATION_SCHEDULES : schedules
    TENANTS ||--o{ USAGE_EVENTS : emits
    NOTIFICATIONS ||--o{ USAGE_EVENTS : measures
    TENANTS ||--o{ WEBHOOK_ENDPOINTS : owns
    WEBHOOK_ENDPOINTS ||--o{ WEBHOOK_DELIVERIES : dispatches
    NOTIFICATIONS ||--o{ WEBHOOK_DELIVERIES : reports
    TENANTS ||--o{ AUDIT_LOGS : audits
    TENANTS ||--o{ DEAD_LETTERS : stores
```

## 4. Table Groups

### 4.1 Tenant And Identity

| Table | Purpose | Important Columns |
| --- | --- | --- |
| `tenants` | Root tenant record | `id`, `name`, `domain` |
| `platform_users` | Human or external identities | `external_user_id`, `email`, `username`, `auth_provider`, `password_hash`, `is_platform_admin` |
| `tenant_memberships` | Tenant-scoped role assignment | `tenant_id`, `platform_user_id`, `role`, `active` |
| `api_keys` | Machine access for tenant APIs | `tenant_id`, `hashed_key`, `created_by_user_id`, `last_used_at` |

### 4.2 Notification Configuration

| Table | Purpose | Important Columns |
| --- | --- | --- |
| `provider_accounts` | Tenant-owned email/SMS provider configuration | `tenant_id`, `channel`, `provider_type`, `configuration_json`, `active` |
| `notification_templates` | Reusable tenant message templates | `tenant_id`, `channel`, `subject_template`, `body_template`, `active` |
| `webhook_endpoints` | Tenant-owned outbound callback configuration | `tenant_id`, `url`, `secret`, `subscribed_events`, `active` |

### 4.3 Notification Execution

| Table | Purpose | Important Columns |
| --- | --- | --- |
| `notifications` | Canonical business record for a notification | `tenant_id`, `recipient`, `channel`, `template`, `template_id`, `provider_account_id`, `idempotency_key`, `subject`, `body`, `scheduled_at`, `status` |
| `delivery_logs` | Attempt-level runtime history | `notification_id`, `tenant_id`, `provider_account_id`, `attempt_no`, `status`, `error_message`, `timestamp` |
| `notification_schedules` | One-time future dispatch state | `tenant_id`, `notification_id`, `scheduled_at`, `status`, `dispatched_at` |
| `dead_letters` | Terminal failure snapshot | `tenant_id`, `recipient`, `channel`, `template`, `status`, `error_message` |

### 4.4 Metering And Governance

| Table | Purpose | Important Columns |
| --- | --- | --- |
| `usage_events` | Immutable event-based usage trail | `tenant_id`, `notification_id`, `event_type`, `quantity`, `metadata_json` |
| `webhook_deliveries` | Retryable outbound webhook dispatches | `tenant_id`, `endpoint_id`, `notification_id`, `event_type`, `status`, `attempts`, `next_attempt_at`, `delivered_at` |
| `audit_logs` | Operational and control-plane audit trail | `tenant_id`, `actor_type`, `actor_id`, `action`, `resource_type`, `resource_id`, `metadata_json` |

## 5. Relationship Notes

### Notifications vs Delivery Logs

This is the most important modeling decision in the repo.

- `notifications` answers: what notification exists, for whom, for what tenant, and what is its current state
- `delivery_logs` answers: how many times did we try, through which provider, and what happened each time

That split keeps retries, status APIs, webhook emission, and future billing logic clean.

### Memberships vs Roles

Roles are not global. They are attached through `tenant_memberships`, which means a user can be:

- admin in one tenant
- member in another tenant
- platform admin across the whole system

### Dead Letters Are Snapshot Records

`dead_letters` currently stores a copy of terminal failure data rather than a direct `notification_id` foreign key. This is acceptable for the current operational design, but it is worth revisiting if DLQ analytics becomes a major product feature.

### Webhooks Are First-Class Operational Entities

Webhook delivery is not just an HTTP side effect. It has dedicated persistence:

- configuration in `webhook_endpoints`
- dispatch state in `webhook_deliveries`
- usage metering in `usage_events`

## 6. Tenant Isolation Model

The current codebase uses `tenant_id` as the application-level isolation key. Every v2 product table is tenant-scoped. In practice this means:

- repository queries filter by `tenant_id`
- request auth resolves a current tenant context
- usage, templates, providers, schedules, and webhooks are all tenant-local

PostgreSQL row-level security is still a future hardening step, so the isolation boundary is strong in application code but not yet enforced by explicit database policies.

## 7. Query Paths The Schema Supports

### Status View

To render a notification status page:

1. read one row from `notifications`
2. read ordered attempts from `delivery_logs`

### Usage View

To render tenant usage:

1. filter `usage_events` by `tenant_id`
2. group by `event_type`, time window, and optionally `channel`

### Webhook Operations

To manage webhook delivery:

1. read active endpoints from `webhook_endpoints`
2. create retryable rows in `webhook_deliveries`
3. emit metering rows to `usage_events`

## 8. Design Summary

The current database design gives NotiX a strong foundation for a SaaS notification platform:

- notification intent is first-class
- tenant data is explicit
- product configuration is modeled directly
- retry, usage, webhook, and audit behavior are all persisted

That combination makes the schema understandable for developers today and extensible for future product work.
