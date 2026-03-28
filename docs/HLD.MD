# High Level Design

## FinTrack AI - Intelligent Personal Finance Management System

**Version:** 1.0  
**Date:** March 28, 2026  
**Status:** Draft  
**Reference:** SRS v1.0

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [System Overview](#2-system-overview)
3. [Architectural Design](#3-architectural-design)
4. [Component Design](#4-component-design)
5. [Technology Stack](#5-technology-stack)
6. [Data Flow](#6-data-flow)
7. [Integration Design](#7-integration-design)
8. [Deployment Overview](#8-deployment-overview)
9. [Non-Functional Design Decisions](#9-non-functional-design-decisions)
10. [Revision History](#10-revision-history)

---

## 1. Introduction

### 1.1 Purpose

This High Level Design (HLD) document describes the overall system architecture, component structure, technology choices, and data flow for FinTrack AI. It translates the requirements defined in the SRS into a concrete architectural blueprint to guide development teams.

### 1.2 Scope

This document covers the architecture of all system layers including the frontend, backend, AI engine, database, and external service integrations. It does not cover implementation-level detail such as class definitions, function signatures, or database schemas, which are addressed in the Low Level Design document.

### 1.3 Audience

This document is intended for system architects, senior developers, technical leads, and DevOps engineers involved in building and deploying FinTrack AI.

### 1.4 Definitions

| Term | Definition |
|------|------------|
| HLD | High Level Design |
| API Gateway | Entry point that routes client requests to appropriate backend services |
| JWT | JSON Web Token used for stateless authentication |
| NLP | Natural Language Processing |
| ORM | Object Relational Mapper |
| CDN | Content Delivery Network |
| CI/CD | Continuous Integration / Continuous Deployment |
| Load Balancer | Distributes incoming network traffic across multiple servers |

---

## 2. System Overview

FinTrack AI is a multi-platform personal finance management system. Users can interact with it through:
- **Web Frontend:** A browser-based interface built with React + Vite.
- **Desktop Application:** A cross-platform desktop client built with JavaFX.
- **Mobile Application:** A native Android application.

All clients communicate with a centralized Spring Boot backend API over HTTPS and WebSocket for real-time updates. The backend coordinates business logic, orchestrates the local Ollama AI engine, manages the PostgreSQL database, and handles caching via Redis.

---

## 3. Architectural Design

### 3.1 Multi-Client Architecture

FinTrack AI follows a distributed architecture supporting multiple client types:

```
+-----------------------------------------------------------+
|                      Presentation Layer                   |
|  [React Web]  |  [JavaFX Desktop]  |  [Android Mobile]    |
+-----------------------------------------------------------+
          |                         ^
          | HTTPS / REST            | WebSocket (Real-time)
          v                         |
+-----------------------------------------------------------+
|                    Application Layer                      |
|            [Spring Boot Backend API Server]               |
+-----------------------------------------------------------+
          |                |                |
          v                v                v
+----------------+  +---------------+  +--------------------+
|   Data Layer   |  |   Cache Layer |  |      AI Layer      |
|  [PostgreSQL]  |  |    [Redis]    |  |  [Ollama (Local)]  |
+----------------+  +---------------+  +--------------------+
```

**Presentation Layer** includes React (Web), JavaFX (Desktop), and Android (Mobile). These clients handle UI rendering, local state, and user interactions.

**Application Layer (Backend)** built with Spring Boot, handles authentication, business logic, data validation, AI orchestration, and notification dispatch.

**Data/Infrastructure Layer** provides persistence (PostgreSQL), caching (Redis), and local AI processing (Ollama).

### 3.2 System Context Diagram

```
+--------------+
|  React Web   | ----\
+--------------+      \      HTTPS / REST      +------------------+      SQL      +------------+
                       +---------------------> |  Spring Boot     | <-----------> | PostgreSQL |
+--------------+      /      WebSocket         |  Backend Server  |               +------------+
| JavaFX App   | ----+                         +------------------+                     ^
+--------------+      \                                 |                               |
                       \      HTTPS / REST              |                               |
+--------------+        +---------------------->        |          +--------------------+
| Android App  |                                        |          |
+--------------+                                        v          v
                                               +-------------------------+
                                               |  Local Infrastructure   |
                                               |  [Redis] | [Ollama AI]  |
                                               +-------------------------+
```

### 3.3 Layered Backend Architecture

Within the Application Tier, the backend is organized into distinct layers:

```
+----------------------------------------------------------+
|                      API Layer (Routes)                  |
+----------------------------------------------------------+
|                   Middleware Layer                        |
|        (Auth, Validation, Rate Limiting, Logging)        |
+----------------------------------------------------------+
|                   Service Layer                           |
|  (Auth | Transaction | Category | Budget | AI | Report)  |
+----------------------------------------------------------+
|                  Repository Layer                         |
|              (Data Access via ORM)                       |
+----------------------------------------------------------+
|                   Database (PostgreSQL)                   |
+----------------------------------------------------------+
```

---

## 4. Component Design

### 4.1 Frontend Application

The frontend is a Single Page Application (SPA) built with React. It is organized into the following modules:

**Authentication Module**
Handles the login, registration, email verification, and password reset screens. Manages storage and renewal of the JWT access token.

**Dashboard Module**
Displays the monthly financial summary including total income, total expenses, net balance, and a snapshot of recent transactions. Renders summary charts.

**Transaction Module**
Provides the transaction list view with search, filter, and pagination. Includes the manual transaction entry form and the natural language input interface.

**Category Module**
Manages the display and editing of predefined and custom categories.

**Budget Module**
Displays active budgets with progress indicators and allows creation and editing of budget entries.

**Reports Module**
Renders the pie chart, bar chart, and line chart views. Provides the date range selector and the CSV export function.

**Settings Module**
Allows the user to update profile information, change password, and configure notification preferences.

**Notification Component**
A persistent component that listens for WebSocket events and displays in-app alerts for budget threshold breaches.

### 4.2 Backend Services

**Authentication Service**
Handles registration, email verification token generation, login, JWT issuance and validation, password reset flow, and token refresh.

**Transaction Service**
Implements all CRUD operations for transactions. Applies business rules such as ensuring the category belongs to the requesting user or is a system default. Handles pagination, filtering, and search query construction.

**Category Service**
Manages creation, retrieval, update, and soft-deletion of categories. On deletion, reassigns orphaned transactions to the system "Uncategorized" category.

**Budget Manager Service**
Creates and updates budgets. After each transaction write operation, this service recalculates category spend totals and compares them against active budgets. Triggers alerts when thresholds are crossed.

**NLP Parser Service**
Receives raw natural language text. Sends a structured prompt to the AI API and parses the response to extract amount, category, and description. Returns a draft transaction object for user confirmation.

**AI Analysis Engine**
Aggregates transaction history from the database and invokes the AI API with a summarization prompt. Handles pattern detection, anomaly identification, and generation of budgeting recommendations. Results are cached in Redis with a TTL to avoid repeated computation.

**Reporting Engine**
Executes aggregation queries against the transaction table. Formats results into the data structures expected by frontend chart components. Handles CSV generation and streaming.

**Notification Service**
Sends transactional emails via the SMTP service for account verification, password reset, and budget breach alerts. Pushes real-time in-app alerts over WebSocket connections for active browser sessions.

### 4.3 Caching Layer (Redis)

Redis is used for two purposes:

- Caching AI analysis results per user with a TTL of 6 hours to reduce redundant API calls.
- Storing WebSocket session mappings to support notification routing when horizontal scaling is applied.

### 4.4 AI Integration

The AI component integrates with an external large language model API (such as Claude or equivalent). It is accessed exclusively through the AI Analysis Engine and NLP Parser Service. Raw user data is summarized and anonymized before being included in prompts. The integration is abstracted behind an internal interface so that the underlying provider can be swapped without modifying other services.

---

## 5. Technology Stack

| Layer | Technology | Rationale |
|-------|------------|-----------|
| Frontend (Web) | React + Vite | Fast, modern component-based UI |
| Desktop | JavaFX | Cross-platform desktop UI with Java |
| Mobile | Android (Kotlin) | Native mobile performance and features |
| Backend Framework | Spring Boot | Enterprise-grade, scalable, mature |
| Database | PostgreSQL | ACID-compliant, strong relational modeling |
| Cache | Redis | Low-latency caching for sessions and AI |
| AI Engine | Ollama (Local LLM) | Privacy-focused, local AI processing |
| Realtime | WebSocket | Low-latency bidirectional updates |
| ORM | Spring Data JPA | Simplified data access with Hibernate |
| Migration | Flyway | Version-controlled database schema |
| Security | JWT | Stateless, secure authentication |
| Containerization | Docker | Consistent multi-service deployment |
| Reverse Proxy | Nginx | TLS termination and request routing |

---

## 6. Data Flow

### 6.1 User Login Flow

```
Client (Any) -> POST /api/auth/login -> Auth Service
Auth Service -> Validate credentials -> PostgreSQL
Auth Service -> Generate JWT -> Return token to Client
Client -> Store token securely
Client -> Connect to WebSocket (ws://) for realtime updates
```

### 6.2 Add Transaction Flow

```
Client -> POST /api/transactions (with JWT) -> Spring Boot Controller
-> Transaction Service -> Validate inputs
-> Repository Layer -> INSERT into PostgreSQL
-> Budget Manager Service -> Recalculate category spend
-> If threshold crossed: Notification Service -> WebSocket push (Realtime)
-> Return created transaction to Client
```

### 6.3 Natural Language Input Flow (Local AI)

```
Client -> POST /api/nlp/parse { text: "Spent 500 on food" }
-> NLP Parser Service -> Build prompt -> Call Local Ollama API
-> Ollama returns structured JSON (e.g. Llama3)
-> NLP Parser extracts amount, category, description
-> Return draft transaction to Client for confirmation
```

### 6.4 AI Analysis Flow

```
Client -> GET /api/analysis/insights
-> AI Analysis Engine -> Check Redis cache
-> Cache hit: Return cached result
-> Cache miss: Query PostgreSQL for transaction history
-> Build prompt -> Call Local Ollama API
-> Ollama returns insights (patterns, recommendations)
-> Store in Redis (TTL 6h) -> Return to Client
```

### 6.5 Report Generation Flow

```
Browser -> GET /api/reports/summary?from=&to=
-> Reporting Engine -> Execute aggregation SQL queries
-> Format response as chart-ready data
-> Return JSON to Browser -> Frontend renders charts
```

---

## 7. Integration Design

### 7.1 Local AI Integration (Ollama)

The AI engine is hosted locally using Ollama. The backend communicates with the Ollama REST API (typically on port 11434). Requests include a system prompt and user data. The model (e.g., Llama3 or Mistral) is configured to return structured JSON for NLP parsing and descriptive text for financial insights.

### 7.2 Email Service Integration

Email is dispatched using Spring Mail configured with an SMTP provider. Templates are stored as Thymeleaf or Freemarker templates. Supported emails include:
- Account verification
- Password reset
- Budget alerts

### 7.3 WebSocket Integration

Real-time updates are handled via standard WebSockets (or STOMP over WebSocket) integrated into Spring Boot. Upon authentication, clients establish a persistent connection. The backend pushes messages for budget warnings, insight completion, and cross-device sync.

---

## 8. Deployment Overview

The system is deployed as a set of containerized services managed by Docker Compose (development) or a cloud container platform (production).

**Services:**

| Service | Container | Port |
|---------|-----------|------|
| Frontend (served by Nginx) | fintrack-frontend | 80 / 443 |
| Backend API | fintrack-backend | 5000 (internal) |
| PostgreSQL | fintrack-db | 5432 (internal) |
| Redis | fintrack-redis | 6379 (internal) |

Nginx acts as the reverse proxy and TLS termination point. All external traffic arrives on port 443 and is proxied to the appropriate internal service. Static frontend assets are served directly by Nginx. API requests are proxied to the backend container.

---

## 9. Non-Functional Design Decisions

### 9.1 Security

- JWT tokens are short-lived (1 hour) with a refresh token mechanism.
- All API routes are protected by authentication middleware except public endpoints (login, register, verify).
- Database credentials and API keys are stored as environment variables, never hardcoded.
- Input sanitization is applied at the middleware layer before data reaches the service layer.

### 9.2 Performance

- Redis caching for AI analysis results reduces repeated external API calls.
- Database indexes are placed on frequently queried columns: user_id, date, category_id, and type in the transactions table.
- Pagination is enforced on all list endpoints to cap response payload size.

### 9.3 Scalability

- The backend is stateless; horizontal scaling is supported by adding additional backend container instances behind the Nginx load balancer.
- Redis handles shared state (sessions, WebSocket mappings) so individual backend instances remain stateless.

### 9.4 Reliability

- PostgreSQL is configured with automated daily backups.
- Redis data is non-critical (cache only); no persistence configuration is required.
- Health check endpoints are exposed on the backend for use by container orchestration or load balancers.

---

## 10. Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | March 28, 2026 | Project Team | Initial draft |