# FinTrack AI - Intelligent Personal Finance Management System

FinTrack AI is a production-grade personal finance management system designed to help users track transactions, manage budgets, and receive real-time financial insights using AI.

## 🚀 Current Status: All Core Modules Implemented — AI-Powered Finance Platform

All planned modules are now complete. The project has evolved from a core finance tracker into an AI-augmented personal finance management system with natural language transaction entry, intelligent analysis, and reporting capabilities. All features are containerized and ready for deployment.

### **Implemented Modules**
- **Authentication Module**: Secure registration, login, email verification, password reset, and profile management with JWT token rotation. Enhanced with custom security handlers (`SecurityAuthenticationEntryPoint`, `SecurityAccessDeniedHandler`), `CustomUserDetails`, Redis-backed rate limiting (Bucket4j), and password blocklist.
- **Transaction Module**: Full CRUD operations for income and expenses with advanced filtering, pagination, and dashboard summaries.
- **Category Module**: System-default and user-defined categories with automatic transaction reassignment on deletion.
- **Budget Module**: Monthly budget tracking with threshold monitoring (80% warning and 100% exceeded alerts).
- **Notification Module**: Real-time alerts via WebSockets (STOMP) and persistent notification storage for security and budget events.
- **NLP Module**: Natural language transaction parsing via Spring AI + Ollama integration, with automatic entity extraction and draft transaction generation.
- **Analysis Module**: AI-driven financial insights, anomaly detection, and spending projections powered by Ollama. Includes spending aggregation, category-based anomaly scoring, and contextual prompt generation.
- **Reporting Module**: Date-range-driven reports for category distribution, monthly trends, and daily spending patterns.

---

## 🛠️ Tech Stack

- **Framework**: Spring Boot 4.0.5
- **Security**: Spring Security with JWT (Access & Refresh Token Rotation)
- **Database**: JPA / Hibernate with H2 (Development) and PostgreSQL (Production ready)
- **Cache & Security**: Redis (for account lockout and rate limiting)
- **AI Integration**: Spring AI + Ollama for NLP parsing and financial analysis
- **Real-time**: WebSocket with STOMP protocol
- **Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Environment**: JDK 21+ (Optimized for JDK 25 compatibility)

---

## ✨ Key Features

- **Production-Grade Security**:
  - SHA-256 hashed refresh tokens.
  - Automatic account lockout after 5 failed attempts (Redis-backed).
  - Password blocklist to prevent insecure credentials.
  - Email verification and secure password reset flow.
  - Custom security entry point and access denied handlers.
- **AI-Powered Insights**: Natural language transaction parsing and intelligent financial analysis via Ollama integration with anomaly detection and spending projections.
- **Resource Isolation**: Every API request is filtered by the authenticated user's ID to ensure strict data ownership.
- **Real-time Notifications**: Immediate alerts for overspending or security events delivered via WebSockets.
- **Reporting & Trends**: Category distribution breakdowns, monthly and daily spending trends with configurable date ranges.
- **Robust Architecture**: Refactored to standard POJOs for JPA entities to ensure stability across modern JDK versions (JDK 25).
- **Containerized Deployment**: Multi-stage Docker build and `docker-compose.yml` for one-command deployment with Redis.

---

## 🏗️ Architecture Overview

The backend follows a clean, modular architecture:
- **Controller Layer**: REST API endpoints with standardized `ApiResponse` envelopes.
- **Service Layer**: Business logic implementation and cross-module orchestration.
- **Repository Layer**: Spring Data JPA for persistent storage.
- **Security Layer**: Custom JWT filter and authentication providers.
- **Common Layer**: Global exception handling and shared DTOs.

---

## 🚦 Getting Started

### **Prerequisites**
- **JDK 21 or 25**
- **Maven 3.9+**
- **Redis Server** (Running on localhost:6379 by default)

### **Setup**
1. Clone the repository.
2. Configure the `.env` file or update `application.yaml` with your credentials:
   - Database connection details.
   - Redis host and port.
   - SMTP server for emails.
   - JWT secret key.

3. Install dependencies:
   ```bash
   mvn clean install
   ```

4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

---

## 🐳 Docker Deployment

Run the entire stack (backend + Redis) with a single command:

```bash
# Configure environment variables
cp .env.example .env   # or create .env manually
# Start all services
docker compose up --build
```

The backend will be available at `http://localhost:8080` and Redis on port `6379`.

---

## 🗺️ Roadmap (Upcoming Enhancements)

- [ ] Frontend application (React/Flutter)
- [ ] CSV import/export for transactions
- [ ] Budget recommendation engine
- [ ] Multi-currency support
- [ ] Mobile push notifications

---

## 📄 Documentation

Detailed specifications can be found in the `docs/` directory:
- [SRS - Software Requirements Specification](docs/SRS.MD)
- [HLD - High Level Design](docs/HLD.MD)
- [LLD - Low Level Design](docs/LLD.MD)
- [API Specification](docs/API.MD)
- [Database Schema](docs/DATABASE.MD)
