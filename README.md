# NotiX – Notification Delivery Service 🚀

A scalable backend system for multi-channel (Email, SMS, Push) notifications. Designed to showcase backend engineering maturity including microservices, retries, observability, and security.

---

## 📦 Architecture Overview

- `api-service`: REST API to send notifications and check status
- `dispatcher-service`: Routes messages to appropriate channel
- `email-sender-service`: Sends emails (mock integration)
- `sms-sender-service`: Sends SMS (mock integration)
- `retry-scheduler-service`: Periodically retries failed messages
- `common-lib`: Shared DTOs/configs
- `infrastructure`: Docker, Prometheus, Grafana config

---

## 📋 Features

- 🔁 Asynchronous message delivery via Kafka/Redis
- 🔐 Secure APIs with JWT or API key auth
- 📈 Metrics + observability (Micrometer + Prometheus + Grafana)
- 📛 Per-client rate limiting
- ♻️ Retry & backoff logic for failed deliveries

---

## 🚀 Getting Started

### Prerequisites
- Docker + Docker Compose
- Java 17
- Kafka or Redis (default is Redis Streams)

### To run locally:
```bash
docker-compose up --build
```

### To test API
Use Postman or cURL to send a POST to `/notifications/send`

---

## 🧪 Testing
Each service contains:
- Unit tests (JUnit5 + Mockito)
- Integration tests (Testcontainers)
- Optional: Spring Cloud Contract for API validation

---

## 🧠 Project Goals

This project was created as part of a 60-day skill-building sprint to demonstrate:
- Real-world microservice architecture
- Resilience, retries, observability, and API design
- Deployment-readiness and clean documentation

---

## 👨‍💻 Author

**Abhishek Gupta**  
Java Backend Engineer • [LinkedIn](https://www.linkedin.com/in/abhishek-gupta-2bbb1919b)  
Project inspired by real-world infrastructure challenges and career growth in cloud-native systems.

---
