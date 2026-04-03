                                                      +-----------------------+
                                                      |   External Clients    |
                                                      |(Web App / Mobile /    |
                                                      |  Third‑Party APIs)    |
                                                      +----------+------------+
                                                                 |
                                                                 v
                                                   +-------------+-------------+
                                                   |       API Gateway /       |
                                                   |     Ingress Controller    |
                                                   |  (Routing, Rate Limit,    |
                                                   |     CORS, TLS Term.)      |
                                                   +-------------+-------------+
                                                                 |
                                        ┌────────────────────────┴────────────────────────┐
                                        │                                                 │
                                        v                                                 v
                         +-----------------------------+                     +-----------------------------+
                         |   Auth Service (OAuth2)     |                     |    Billing Service         |
                         |  (Token Issuance, RBAC)     |                     | (Usage Metering, Plans,    |
                         +-------------+---------------+                     |   Invoicing & Webhooks)    |
                                       |                                     +-------------+---------------+
                                       |                                                   |
                +----------------------+----------------------+                            |
                |                                             |                            |
                v                                             v                            v
  +-----------------------------+               +-----------------------------+   +-----------------------------+
  |       API Service           |               |   Tenant Management Service |   |    Dashboard Service        |
  |  (Tenant-ID header check,   |               | (Onboarding, Settings,      |   | (React/Vue UI + REST APIs   |
  |   Input Validation, Swagger)|               |  Custom Domains, Metadata)  |   |  for usage + Grafana embeds)|
  +-------------+---------------+               +-------------+---------------+   +-------------+---------------+
                |                                             |                            |
                v                                             v                            |
         +------+---------+                        +----------+----------+                |
         |   Dispatcher   |                        |   Tenant Config DB   |                |
         |   Service      |                        |   (Tenants, Plans,   |                |
         +------+---------+                        |    API Keys, Quotas) |                |
                |                                 +----------+-----------+                |
                v                                             |                            |
    +-----------+-----------+                                 v                            |
    | Kafka Cluster (Multi‑  |                     +----------+-----------+                |
    | tenant topics:         |                     |  Billing Database    |                |
    | notifications.{tID}.*) |                     |   (Usage Records,    |                |
    +-----------+-----------+                     |    Invoices)         |                |
                |                                 +----------+-----------+                |
      ┌─────────┴────────┐                                       |                         |
      │                  │                                       |                         |
      v                  v                                       |                         |
+-----+-----+      +-----+-----+                        +--------+--------+        +-------+-------+
| Email     |      | SMS       |                        | PostgreSQL (Multi-  |        | Grafana /    |
| Sender    |      | Sender    |                        | tenant via row‑level)|        | OpenTelemetry|
+-----------+      +-----------+                        +---------------------+        +---------------+
      |                  |                                                           |
      v                  v                                                           |
+-----+-----+      +-----+-----+                                                     |
| Delivery  |      | DLQ       |                                                     |
| Logs      |      | (dead_letters)                                                   |
+-----------+      +-----------+                                                     |
                                                                                +----+-----+
                                                                                | Metrics   |
                                                                                | DB & Tracing |
                                                                                +----------+
