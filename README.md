# Reliable Order Platform

A production-oriented backend application built using **Java 21, Spring Boot, PostgreSQL, and Apache Kafka**. The project demonstrates reliable asynchronous communication between services, transactional event publishing, idempotent message processing, and failure handling.

The primary goal is to build a reliable order-processing system that maintains data consistency between a relational database and an event-driven messaging platform.

## Key Features

- **RESTful APIs:** Create and retrieve orders using Spring Boot REST controllers.
- **Transactional Outbox Pattern:** Ensures order creation and event persistence happen within the same database transaction.
- **Asynchronous Messaging:** Uses Apache Kafka to publish and consume order events.
- **Idempotent Consumer:** Prevents duplicate event processing using a unique event ID and PostgreSQL.
- **Dead Letter Queue (DLQ):** Routes messages to a DLQ after configured processing retries are exhausted.
- **Database Transactions:** Maintains consistency between event processing and order status updates.
- **Retry Mechanism:** Retries failed Kafka message processing before sending messages to the DLQ.
- **Containerized Infrastructure:** Runs PostgreSQL and Kafka using Docker Compose.
- **Automated Testing:** Includes unit and integration tests for business logic and event-processing scenarios.

## Technology Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Backend Framework | Spring Boot |
| Build Tool | Maven |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka 3.9.1 |
| ORM | Spring Data JPA / Hibernate |
| Containerization | Docker & Docker Compose |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| API Testing | Postman |
| Version Control | Git |

## Architecture Overview

The application follows an event-driven architecture using the Transactional Outbox Pattern.

```text
                  Client
                    |
                    |
                    v
             REST API
          POST /api/orders
                    |
                    v
              Order Service
                    |
             Database Transaction
                    |
             +------+------+
             |             |
             v             v
         Orders Table   Outbox Table
             |             |
             |             |
             +------+------+
                    |
                    v
              Outbox Publisher
                    |
                    v
                Kafka
              order-events
                    |
                    v
           Kafka Event Consumer
                    |
                    v
          Idempotency Validation
                    |
                    v
          Processed Events Table
                    |
                    v
          Update Order Status
                    |
                    v
           PostgreSQL Database


        Failed Message Processing
                    |
                    v
             Retry Mechanism
                    |
                    v
             Dead Letter Queue
             order-events.DLQ
```

## How It Works

### 1. Order Creation

The client sends a request to create an order through the REST API.

The Order Service performs the following operations within a single database transaction:

1. Creates a new order with `PENDING` status.
2. Generates an order-created event with a unique event ID.
3. Serializes the event into JSON.
4. Stores the event in the Outbox table with `PENDING` status.
5. Commits both database changes together.

This ensures that an order cannot be successfully created without its corresponding event being persisted in the Outbox table.

### 2. Transactional Outbox Publishing

A scheduled Outbox Publisher periodically checks for unpublished events.

1. Retrieves pending events from the Outbox table.
2. Publishes the event to the Kafka `order-events` topic.
3. Waits for Kafka's acknowledgement.
4. Updates the Outbox event status to `PUBLISHED` after successful publishing.

If publishing fails, the event remains available for another publishing attempt.

**Why the Transactional Outbox Pattern?**

Publishing directly to Kafka inside a database transaction does not guarantee atomicity between PostgreSQL and Kafka.

The Outbox Pattern solves this problem by persisting the business data and event in the same database transaction. A separate publisher handles communication with Kafka.

### 3. Kafka Event Consumption

The Kafka consumer listens to the `order-events` topic.

When an event arrives:

1. Reads and validates the event payload.
2. Extracts the unique event ID and order details.
3. Checks whether the event has already been processed.
4. Updates the order status from `PENDING` to `CONFIRMED`.
5. Records the event in the `processed_events` table.

The event record and order status update are performed within a single database transaction.

### 4. Idempotent Event Processing

Kafka consumers can receive duplicate messages, especially when a consumer processes a message but fails before its offset is committed.

To handle this scenario, the application implements idempotent event processing.

- Every event contains a unique `eventId`.
- The `processed_events` table stores successfully processed event IDs.
- PostgreSQL's `INSERT ... ON CONFLICT DO NOTHING` provides atomic duplicate detection.
- If the event already exists, the consumer ignores it.
- If processing fails, the transaction rolls back, including the processed-event insert.

This prevents the same event from applying its business operation multiple times.

### 5. Retry and Dead Letter Queue

The application uses Spring Kafka's `DefaultErrorHandler` with a `DeadLetterPublishingRecoverer`.

Current retry configuration:

- Retry interval: 2 seconds.
- Retry attempts: 3 after the initial attempt.
- Maximum listener invocations: 4.
- Failed messages are published to `order-events.DLQ` after retries are exhausted.

The DLQ allows failed messages to be inspected and investigated without blocking normal message processing indefinitely.

## Project Structure

```text
reliable-order-platform/
│
├── infrastructure/
│   └── compose.yaml
│
├── services/
│   └── order-service/
│       │
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/example/orders/
│       │   │   │   │
│       │   │   │   ├── config/
│       │   │   │   │   └── KafkaConsumerConfig.java
│       │   │   │   │
│       │   │   │   ├── controller/
│       │   │   │   │   └── OrderController.java
│       │   │   │   │
│       │   │   │   ├── consumer/
│       │   │   │   │   ├── OrderEventConsumerService.java
│       │   │   │   │   ├── OrderEventKafkaListener.java
│       │   │   │   │   ├── ProcessedEvent.java
│       │   │   │   │   └── ProcessedEventRepository.java
│       │   │   │   │
│       │   │   │   ├── dto/
│       │   │   │   │   ├── CreateOrderRequest.java
│       │   │   │   │   ├── OrderResponse.java
│       │   │   │   │   └── ErrorResponse.java
│       │   │   │   │
│       │   │   │   ├── entity/
│       │   │   │   │   ├── Order.java
│       │   │   │   │   └── OrderStatus.java
│       │   │   │   │
│       │   │   │   ├── event/
│       │   │   │   │   └── OrderCreatedEvent.java
│       │   │   │   │
│       │   │   │   ├── exception/
│       │   │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   │   └── OrderNotFoundException.java
│       │   │   │   │
│       │   │   │   ├── outbox/
│       │   │   │   │   ├── OutboxEvent.java
│       │   │   │   │   ├── OutboxEventRepository.java
│       │   │   │   │   └── OutboxPublisher.java
│       │   │   │   │
│       │   │   │   ├── repository/
│       │   │   │   │   └── OrderRepository.java
│       │   │   │   │
│       │   │   │   ├── service/
│       │   │   │   │   └── OrderService.java
│       │   │   │   │
│       │   │   │   └── OrderServiceApplication.java
│       │   │   │
│       │   │   └── resources/
│       │   │       └── application.properties
│       │   │
│       │   └── test/
│       │
│       ├── Dockerfile
│       ├── compose.yaml
│       └── pom.xml
│
├── pom.xml
└── README.md
```

## Getting Started

### Prerequisites

Make sure the following tools are installed:

- Java 21
- Docker Desktop
- Git
- IntelliJ IDEA (recommended)

Maven is included through the Maven Wrapper.

### 1. Clone the Repository

```bash
git clone <your-repository-url>
cd reliable-order-platform
```

### 2. Start Infrastructure

Start PostgreSQL and Kafka using Docker Compose:

```bash
docker compose -f infrastructure/compose.yaml up -d
```

Verify that the containers are running:

```bash
docker ps
```

### 3. Run the Order Service

Navigate to the order-service directory:

```powershell
cd services/order-service
```

Start the Spring Boot application:

```powershell
.\mvnw.cmd spring-boot:run
```

The application runs at:

```text
http://localhost:8080
```

### 4. Stop Infrastructure

From the project root:

```bash
docker compose -f infrastructure/compose.yaml down
```

To remove containers and their associated named volumes:

```bash
docker compose -f infrastructure/compose.yaml down -v
```

**Note:** The `-v` option removes persisted database data. Use it only when you intentionally want to reset the local environment.

## API Endpoints

### Create Order

**Endpoint**

```http
POST /api/orders
```

**Request Body**

```json
{
  "productId": "PRODUCT-101",
  "quantity": 2
}
```

**Successful Response — 201 Created**

```json
{
  "id": 1,
  "productId": "PRODUCT-101",
  "quantity": 2,
  "status": "PENDING",
  "createdAt": "2026-11-03T10:30:00Z"
}
```

### Get Order By ID

**Endpoint**

```http
GET /api/orders/{id}
```

**Example**

```http
GET /api/orders/1
```

**Successful Response — 200 OK**

```json
{
  "id": 1,
  "productId": "PRODUCT-101",
  "quantity": 2,
  "status": "CONFIRMED",
  "createdAt": "2026-11-03T10:30:00Z"
}
```

The order is initially created with `PENDING` status. After the Kafka event is successfully consumed and processed, the status changes to `CONFIRMED`.

## Kafka Topics

| Topic | Purpose |
|---|---|
| `order-events` | Publishes order-related events |
| `order-events.DLQ` | Stores messages that fail processing after retries |

### Example Event Payload

```json
{
  "eventId": "unique-event-id",
  "eventType": "ORDER_CREATED",
  "orderId": 1,
  "productId": "PRODUCT-101",
  "quantity": 2,
  "occurredAt": "2026-11-03T10:30:00Z"
}
```

## Database Tables

| Table | Responsibility |
|---|---|
| `orders` | Stores order information and current status |
| `outbox_event` | Stores events waiting to be published to Kafka |
| `processed_events` | Tracks processed event IDs to prevent duplicate processing |

## Testing

Run the tests from the order-service directory:

```powershell
.\mvnw.cmd test
```

The project includes tests covering:

- Order creation and retrieval.
- Order-not-found scenarios.
- Event processing and order status updates.
- Duplicate event detection.
- Transaction rollback when event processing fails.
- Kafka-related failure-handling scenarios.

## Engineering Concepts Demonstrated

This project focuses on backend engineering concepts commonly used in distributed systems:

1. REST API design and request validation.
2. Spring Boot dependency injection and application configuration.
3. Database transactions and transaction boundaries.
4. Transactional Outbox Pattern.
5. Event-driven architecture.
6. Kafka producers and consumers.
7. Message acknowledgement and retry handling.
8. Idempotency and duplicate message processing.
9. Dead Letter Queue and failure recovery.
10. Unit and integration testing.
11. Docker-based infrastructure management.

## Future Improvements

The following enhancements can make the platform more production-ready:

- Implement concurrent-safe Outbox publishing using PostgreSQL `FOR UPDATE SKIP LOCKED`.
- Introduce an Outbox event claiming mechanism with lease-based recovery.
- Add exponential backoff and configurable retry policies.
- Implement centralized logging and distributed tracing.
- Add Prometheus metrics and Grafana dashboards.
- Introduce API authentication and authorization.
- Add Kubernetes deployment configurations.
- Expand the platform with Inventory and Payment services.
- Implement distributed tracing using OpenTelemetry.

## Author

**Ayush Chandrawar**

Software Engineer | Java | Spring Boot | Microservices | Kafka

---

*Built as a hands-on backend engineering project to explore reliable event-driven systems, distributed transactions, and fault-tolerant message processing.*
