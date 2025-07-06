# 🔔 NotiX - Notification Delivery System (POC v1.0)

NotiX is a lightweight, distributed notification delivery system designed to handle **Email** and **SMS** communication at scale using **Kafka**-based asynchronous messaging.

> ✅ This is the **v1.0 POC Release**, built to showcase core functionality, extensibility, and resilience.

---

## 🔧 Tech Stack

- **Java 21 + Spring Boot 3.5.0**
- **Apache Kafka**
- **PostgreSQL**
- **Prometheus + Grafana**
- **Twilio (SMS) + MailSender (Email)**
- **Spring Actuator, SpringDoc (Swagger UI)**
- **Custom Rate Limiter + API Key Auth**

---

## 🧩 Microservices Overview

```
                                 +---------------------+
                                 |     API SERVICE     |
                                 |  /notifications/send|
                                 +---------------------+
                                            |
                                            v
                              [Kafka Topic: notifications]
                                            |
                                            v
                                 +---------------------+
                                 | DISPATCHER SERVICE  |
                                 |  Routes by channel  |
                                 +---------------------+
                                /                        \
                     [Topic: notifications.email]    [Topic: notifications.sms]
                    /                                   \
           +---------------------+             +---------------------+
           | EMAIL SERVICE       |             | SMS SERVICE         |
           | Sends Email         |             | Sends SMS via Twilio|
           +---------------------+             +---------------------+
                      \                        /
                    [PostgreSQL Delivery Logs]
                             |
                             v
                +------------------------------+
                | RETRY SCHEDULER SERVICE      |
                | Retries failed notifications |
                | Moves final failures to DLQ  |
                +------------------------------+
```

---

## ✅ Features Implemented (v1.0)

### 📬 Notification Flow
- REST API to send notifications (`/notifications/send`)
- Kafka-backed message delivery
- Channel-based routing (EMAIL/SMS)
- Async delivery via Kafka producers/consumers

### 💾 Persistence & Retry
- Delivery logs stored in PostgreSQL
- Retry mechanism for failed messages
- Dead Letter Queue (DLQ) for maxed out retries

### 🔍 Observability
- Spring Boot Actuator + Prometheus Metrics
- Custom Kafka, Retry, DLQ Metrics
- Grafana Dashboards (JVM + Custom Alerts)

### 🔐 Security Layer
- API Key-based Filter Authentication
- Rate Limiter using Bucket4j
- Swagger UI with OpenAPI documentation

---

## 📦 Modules

| Module | Description |
|--------|-------------|
| `api-service` | Exposes REST endpoints for clients |
| `dispatcher-service` | Kafka consumer to route messages |
| `email-sender-service` | Sends emails via Spring Mail |
| `sms-sender-service` | Sends SMS via Twilio |
| `retry-scheduler-service` | Retries failed deliveries & handles DLQs |
| `common-lib` | Shared DTOs, enums, and utility classes |

---

## 📖 API Reference

### Send Notification

```http
POST /notifications/send
```

**Payload**
```json
{
  "id": "uuid-here",
  "to": "recipient@example.com",
  "channel": "EMAIL",
  "template": "WELCOME_TEMPLATE",
  "params": {
    "username": "Abhishek"
  }
}
```

### Check Status

```http
GET /notifications/status/{id}
```

---

## 🔍 Swagger Docs

Visit: `http://localhost:7070/swagger-ui.html`

---

## 🛠️ Local Setup (Dev Mode)

> Prerequisite: Docker, JDK 21, Maven

```bash
# Clone the repo
git clone https://github.com/abhishekwork07/NotiX--Notification-Delivery-Service.git
cd NotiX--Notification-Delivery-Service

# Start Kafka + Postgres + Grafana + Prometheus
docker-compose up -d

# Start each microservice individually
cd api-service && mvn spring-boot:run
cd dispatcher-service && mvn spring-boot:run
...
```

---

## 📊 Dashboards

- **Prometheus**: [http://localhost:9090/targets](http://localhost:9090/targets)
- **Grafana**: [http://localhost:3000](http://localhost:3000)
  - 🔑 Username/Password: `admin/admin`
  - 📈 Dashboards: JVM, Retry Metrics, DLQ Overview

---

## 💡 Next Steps (Planned for SaaS Upgrade)

- JWT/OAuth2 based Authentication
- Tenant-level Isolation
- Notification Quota Management & Billing
- Kafka Partitioning by Tenant
- UI Dashboard for tenants

---

## 📂 Repo

🔗 GitHub: [NotiX Notification Delivery Service](https://github.com/abhishekwork07/NotiX--Notification-Delivery-Service)

---

## 👨‍💻 Built With Love by **Abhishek Gupta**

Let’s build scalable & elegant notification systems.
