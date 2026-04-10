# common

`common` is the shared contract module for NotiX.

## Responsibilities

- hold DTOs shared across services
- hold enums shared across services
- stay free of service-specific JPA entities
- compile as a reusable library jar for the rest of the repo and for external local projects

## Main Contracts

- `SendRequest`
  - v1 public request payload
- `NotificationEvent`
  - async ingress and retry event
- `NotificationStatusEvent`
  - delivery outcome event sent back to the API layer
- `Channel`
  - notification channel enum
- `Status`
  - notification and attempt lifecycle enum
- `NotificationLifecycleEventType`
  - semantic event type for status fan-in and webhooks

## Why This Module Exists

Without `common`, each service would need duplicate transport classes and enums. Keeping contracts here ensures:

- producers and consumers agree on the same message shape
- event evolution stays coordinated
- external local tools can depend on the same shared jar if needed

## Notes

- `common` is intentionally not a Spring Boot application
- JPA entities remain local to the owning service modules

## Build

```bash
./mvnw -f pom.xml install -DskipTests
```

Run from inside `common/`.

## Read Next

- [Root README](../README.md)
- [Low-Level Design](../docs/LLD.md)
