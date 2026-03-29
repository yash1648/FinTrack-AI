# FinTrack AI - Intelligent Personal Finance Management System

FinTrack AI is a production-grade personal finance management system designed to help users track transactions, manage budgets, and receive real-time financial insights using AI.

## 🚀 Current Status: Core Infrastructure & Financial Modules Complete

The project has successfully implemented the core backend infrastructure and primary financial management modules. All implemented features strictly adhere to the project's [LLD](docs/LLD.MD) and [API](docs/API.MD) specifications.

### **Implemented Modules**
- **Authentication Module**: Secure registration, login, email verification, password reset, and profile management.
- **Transaction Module**: Full CRUD operations for income and expenses with advanced filtering and pagination.
- **Category Module**: System-default and user-defined categories with automatic transaction reassignment on deletion.
- **Budget Module**: Monthly budget tracking with threshold monitoring (80% warning and 100% exceeded alerts).
- **Notification Module**: Real-time alerts via WebSockets (STOMP) and persistent notification storage for security and budget events.

---

## 🛠️ Tech Stack

- **Framework**: Spring Boot 4.0.5
- **Security**: Spring Security with JWT (Access & Refresh Token Rotation)
- **Database**: JPA / Hibernate with H2 (Development) and PostgreSQL (Production ready)
- **Cache & Security**: Redis (for account lockout and rate limiting)
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
- **Resource Isolation**: Every API request is filtered by the authenticated user's ID to ensure strict data ownership.
- **Real-time Notifications**: Immediate alerts for overspending or security events delivered via WebSockets.
- **Robust Architecture**: Refactored to standard POJOs for JPA entities to ensure stability across modern JDK versions (JDK 25).

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

## 🗺️ Roadmap (Upcoming Modules)

- [ ] **NLP Parser Module**: Integration with Ollama for natural language transaction entry.
- [ ] **AI Analysis Module**: Financial pattern recognition and spending recommendations.
- [ ] **Reporting Module**: Monthly comparisons, category distribution charts, and CSV exports.

---

## 📄 Documentation

Detailed specifications can be found in the `docs/` directory:
- [SRS - Software Requirements Specification](docs/SRS.MD)
- [HLD - High Level Design](docs/HLD.MD)
- [LLD - Low Level Design](docs/LLD.MD)
- [API Specification](docs/API.MD)
- [Database Schema](docs/DATABASE.MD)
