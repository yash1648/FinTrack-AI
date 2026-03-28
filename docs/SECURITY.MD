# Security Design

## FinTrack AI - Intelligent Personal Finance Management System

**Version:** 1.0  
**Date:** March 28, 2026  
**Status:** Draft  
**Reference:** SRS v1.0, HLD v1.0, API v1.0

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Security Objectives](#2-security-objectives)
3. [Authentication and Authorization](#3-authentication-and-authorization)
4. [Data Protection](#4-data-protection)
5. [API Security](#5-api-security)
6. [Input Validation and Injection Prevention](#6-input-validation-and-injection-prevention)
7. [Session Management](#7-session-management)
8. [AI Component Security](#8-ai-component-security)
9. [Infrastructure Security](#9-infrastructure-security)
10. [Monitoring and Incident Response](#10-monitoring-and-incident-response)
11. [Security Checklist](#11-security-checklist)
12. [Revision History](#12-revision-history)

---

## 1. Introduction

### 1.1 Purpose

This Security Design document defines the security controls, policies, and implementation guidelines for FinTrack AI. It addresses authentication, authorization, data protection, communication security, input handling, infrastructure hardening, and incident response. It is intended to ensure that user financial data is protected from unauthorized access, disclosure, tampering, and loss.

### 1.2 Scope

This document covers all layers of the FinTrack AI system: frontend, backend API, database, AI integration, and deployment infrastructure. It applies to all engineers, DevOps personnel, and third-party integrations involved in the system.

### 1.3 Compliance Basis

Security controls are designed in alignment with the following references:

- OWASP Top 10 (2021 edition)
- OWASP API Security Top 10
- General data privacy principles applicable in India (DPDP Act 2023 awareness)
- NIST Cybersecurity Framework (informative reference)

---

## 2. Security Objectives

| Objective | Description |
|-----------|-------------|
| Confidentiality | User financial data must not be accessible by unauthorized parties, including other users of the system. |
| Integrity | Financial records must not be modified without user authorization. All inputs must be validated before processing. |
| Availability | The system must remain accessible to legitimate users. Protections against denial-of-service conditions must be in place. |
| Authentication | Every protected action must require a verified user identity. |
| Non-repudiation | Critical actions (transaction creation, deletion, password changes) must be logged with user attribution and timestamps. |
| Least Privilege | Each system component must operate with only the permissions required for its function. |

---

## 3. Authentication and Authorization

### 3.1 Password Policy

All user passwords must satisfy the following requirements at the time of registration and on every password change:

- Minimum length: 8 characters
- Must contain at least one uppercase letter (A-Z)
- Must contain at least one lowercase letter (a-z)
- Must contain at least one digit (0-9)
- Must contain at least one special character (!@#$%^&*...)
- Must not be the same as the previous password
- Must not match common password patterns (validated against a blocklist of the 10,000 most common passwords)

### 3.2 Password Storage

Passwords are never stored in plaintext or in reversibly encrypted form. The bcrypt hashing algorithm is used with a work factor (cost) of 12. This value must be reviewed annually and increased as hardware performance improves.

The stored hash format is: `$2b$12$<salt><hash>` (bcrypt output).

The password_hash column in the users table stores this value exclusively. No intermediate values, temporary hashes, or plaintext representations are written to logs, error messages, or any other storage medium.

### 3.3 JWT Access Token

Access tokens are issued on successful login using the RS256 or HS256 algorithm with a secret of at least 256 bits.

| Property | Value |
|----------|-------|
| Algorithm | HS256 (HMAC-SHA256) |
| Expiry | 1 hour |
| Payload claims | userId, email, iat, exp |
| Transport | Authorization: Bearer header |
| Storage (client) | In-memory only (not localStorage or sessionStorage) |

Access tokens must never be logged, included in URL parameters, or stored in browser storage accessible to JavaScript.

### 3.4 Refresh Token

Refresh tokens allow obtaining new access tokens without re-authentication.

| Property | Value |
|----------|-------|
| Format | Cryptographically random 32-byte hex string |
| Storage (server) | SHA-256 hash stored in refresh_tokens table |
| Expiry | 7 days |
| Rotation | Token is invalidated and a new one issued on each use |
| Revocation | Token is deleted from DB on logout or password change |

Rotation prevents refresh token reuse attacks. If a previously used refresh token is presented, the server invalidates all refresh tokens for that user (token theft indicator).

### 3.5 Authorization Model

FinTrack AI uses resource-level ownership authorization. There are no roles beyond authenticated user and system administrator.

**Rules:**

- Every API request to a protected endpoint must include a valid access token.
- The authenticated userId is extracted from the token and used as a filter on all database queries. A user can only read or modify their own resources.
- System administrators access the database directly and do not have a privileged API role in v1.0.
- Category ownership is checked before any transaction is associated with a category.
- Attempts to access another user's resource return HTTP 404 (not 403) to prevent resource enumeration.

### 3.6 Account Lockout

To protect against brute-force login attacks:

- After 5 consecutive failed login attempts within 10 minutes for a given email, the account is locked for 15 minutes.
- The lockout duration doubles with each subsequent lockout for the same account within a 24-hour window.
- A notification email is sent to the account owner when a lockout occurs.
- Lockout state is tracked in Redis: key = "lockout:{email}", value = { attempts, lockedUntil }.

---

## 4. Data Protection

### 4.1 Encryption in Transit

- All client-to-server communication uses TLS 1.2 or higher. TLS 1.0 and 1.1 are explicitly disabled on the Nginx configuration.
- HTTP requests on port 80 are redirected to HTTPS via a 301 response.
- Recommended cipher suites prioritize forward secrecy (ECDHE-based key exchange).
- TLS certificates are obtained via Let's Encrypt and renewed automatically before expiry.

### 4.2 Encryption at Rest

- The PostgreSQL database volume is encrypted at the infrastructure level using the cloud provider's disk encryption (AES-256).
- Backups stored in cloud object storage are encrypted using server-side encryption with provider-managed keys.
- No application-level column encryption is applied in v1.0. Sensitive columns (password_hash, token_hash) are protected through hashing rather than encryption.

### 4.3 Sensitive Data Handling

The following data is classified as sensitive and subject to strict handling rules:

| Data | Classification | Handling |
|------|---------------|----------|
| password_hash | Critical | Never logged, never returned in API responses |
| verification_token | Sensitive | Stored in DB, cleared after use, never returned in list responses |
| token_hash (refresh/reset) | Sensitive | Stored as hash only, raw token sent in email only |
| User financial transactions | Private | Returned only to the owning user |
| AI prompt content | Private | Aggregated data only, no raw records in prompts |

### 4.4 Data Minimization

API responses include only the fields required by the client. Internal fields such as password_hash, verification_token, and token_hash columns are never included in API responses. The repository layer explicitly selects only required columns for each query.

---

## 5. API Security

### 5.1 HTTPS Enforcement

The Nginx reverse proxy enforces HTTPS. The backend application server listens only on the internal network and is not directly accessible from the internet.

### 5.2 CORS Policy

Cross-Origin Resource Sharing is configured on the backend to allow requests only from the registered frontend domain:

```
Access-Control-Allow-Origin: https://fintrack.ai
Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Allow-Credentials: true
```

Wildcard origins (*) are strictly prohibited.

### 5.3 Security Headers

The following HTTP security headers are set on all responses by the Nginx layer:

| Header | Value | Purpose |
|--------|-------|---------|
| Strict-Transport-Security | max-age=31536000; includeSubDomains | Force HTTPS |
| X-Content-Type-Options | nosniff | Prevent MIME sniffing |
| X-Frame-Options | DENY | Prevent clickjacking |
| Content-Security-Policy | default-src 'self' | Restrict resource origins |
| Referrer-Policy | no-referrer | Prevent referrer leakage |
| Permissions-Policy | geolocation=(), microphone=() | Disable unused browser features |

### 5.4 Rate Limiting

Rate limiting is enforced at the API Gateway layer using express-rate-limit backed by Redis. Limits are defined per endpoint group in the API Specification document. On limit breach, HTTP 429 is returned with a Retry-After header.

### 5.5 CSRF Protection

Since the application uses stateless JWT authentication passed in the Authorization header (not cookies), traditional CSRF attacks are not applicable to API endpoints.

If cookies are used for refresh token storage, the httpOnly and SameSite=Strict attributes are set to prevent JavaScript access and cross-site submission.

---

## 6. Input Validation and Injection Prevention

### 6.1 Input Validation Strategy

All input is validated at two layers:

**Layer 1 - Controller (Request Entry):** Jakarta Bean Validation (Hibernate Validator) annotations (e.g., `@NotBlank`, `@Email`, `@Positive`) on DTOs validate request bodies before business logic executes. Invalid requests are rejected with HTTP 422 by the global exception handler.

**Layer 2 - Persistence (Database Entry):** Spring Data JPA (Hibernate) uses prepared statements and parameterized queries exclusively. No raw string concatenation is used to build SQL queries.

### 6.2 SQL Injection Prevention

Spring Data JPA generates parameterized SQL for all database interactions. Prohibited pattern:

```java
// NEVER do this
String query = "SELECT * FROM transactions WHERE user_id = '" + userId + "'";
```

Required pattern:

```java
// Always use parameterized queries or derived query methods
@Query("SELECT t FROM Transaction t WHERE t.user.id = :userId")
List<Transaction> findByUserId(@Param("userId") Long userId);
```

### 6.3 Cross-Site Scripting (XSS) Prevention

- The frontend uses React, which escapes all rendered values by default. Direct DOM manipulation using innerHTML is prohibited.
- The backend does not render HTML. All API responses are JSON. The Content-Type header is always application/json.
- The Content-Security-Policy header restricts the execution of inline scripts.
- User-supplied text (descriptions, names) is stored and returned as-is. Sanitization for display is the responsibility of the React rendering layer (handled automatically by JSX).

### 6.4 Mass Assignment Prevention

API endpoints accept only explicitly whitelisted fields. Zod schemas define the exact set of accepted fields per endpoint. Any additional fields in the request body are stripped before processing. The userId is always sourced from the verified JWT token, never from the request body.

### 6.5 File Upload Security

FinTrack AI v1.0 does not support file uploads. The CSV export feature streams data to the client; no files are uploaded by users.

---

## 7. Session Management

### 7.1 Token Storage on Client

- The access token is stored in JavaScript memory (React state / Redux store) only. It is not written to localStorage, sessionStorage, or cookies.
- The refresh token is stored in an httpOnly, Secure, SameSite=Strict cookie. This prevents JavaScript access while allowing automatic inclusion in refresh token requests.

### 7.2 Session Expiry

- Access tokens expire after 1 hour.
- Refresh tokens expire after 7 days.
- If a user is inactive and the access token expires, the client silently attempts a refresh using the refresh token cookie. If the refresh token is also expired or revoked, the user is redirected to the login screen.

### 7.3 Logout

On logout:

1. The backend deletes the refresh token record from the database.
2. The httpOnly cookie is cleared by setting it with an expired date.
3. The frontend clears the access token from memory and Redux state.
4. The user is redirected to the login screen.

### 7.4 Concurrent Session Handling

In v1.0, multiple simultaneous sessions are permitted (e.g., mobile and desktop). Each session has its own refresh token stored in the database. Logout from one session does not invalidate others. A "logout all devices" feature is planned for v1.1 and will be implemented by deleting all refresh tokens for the user.

---

## 8. AI Component Security

### 8.1 Local AI Execution (Ollama)

FinTrack AI uses **Ollama** for local AI processing. This provides a significant security and privacy advantage over cloud-based LLMs:

- **Data Sovereignty:** Financial data never leaves the controlled infrastructure. No data is sent to third-party AI providers (e.g., Anthropic, OpenAI).
- **Network Isolation:** The Ollama service runs in a container accessible only to the backend on the internal Docker network. It is not exposed to the internet.
- **Privacy by Design:** User transaction descriptions used for NLP parsing and history used for insights remain within the application's secure boundary.

### 8.2 Model Integrity

- Only official, verified model weights (e.g., Llama3, Mistral) from the Ollama library are used.
- Model versions are pinned in the deployment configuration to ensure consistent and predictable behavior.

### 8.3 AI Response Validation

AI responses are treated as untrusted input and validated before use:

- **NLP Parsing:** The JSON response from Ollama is parsed and validated against the `DraftTransactionDTO` schema. Invalid or malicious JSON is rejected.
- **Insights:** AI-generated text is rendered as plain text or safe Markdown on the frontend. No executable code or scripts are ever extracted from AI responses.
- **Prompt Injection:** User input is treated as data within a structured prompt template. System instructions are separated from user data to prevent prompt injection attacks.

---

## 9. Infrastructure Security

### 9.1 Environment Variables

All secrets and configuration values are stored as environment variables. They are never hardcoded in source code or committed to version control.

Required environment variables:

| Variable | Description |
|----------|-------------|
| DATABASE_URL | PostgreSQL connection string |
| JWT_SECRET | Secret for signing access tokens |
| JWT_REFRESH_SECRET | Secret for signing refresh tokens |
| AI_API_KEY | API key for the AI provider |
| SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS | Email service credentials |
| REDIS_URL | Redis connection string |
| BASE_URL | Frontend application base URL |

Environment files (.env) are listed in .gitignore and are never committed.

### 9.2 Server Hardening

The production server follows these baseline hardening steps:

- SSH access is restricted to key-based authentication only. Password-based SSH login is disabled.
- The root user login is disabled over SSH.
- A non-root user with sudo access is used for application deployment.
- Unnecessary services and ports are disabled. Only ports 80, 443, and 22 (SSH) are exposed externally.
- Unattended security updates are enabled on Ubuntu.
- A firewall (ufw) is configured to allow only the above ports.

### 9.3 Container Security

- Docker containers run as non-root users.
- Official base images from Docker Hub are used with pinned versions.
- The Docker socket is not exposed to application containers.
- No secrets are baked into Docker images. Secrets are injected at runtime via environment variables.

### 9.4 Database Security

- PostgreSQL is not exposed externally. It listens only on the internal Docker network.
- The application connects using a dedicated database user with SELECT, INSERT, UPDATE, DELETE permissions on application tables only. It does not have CREATE TABLE, DROP, or SUPERUSER privileges.
- The PostgreSQL superuser password is set and documented in the internal secrets vault.

### 9.5 Dependency Management

- **Backend:** Maven or Gradle pins all dependencies. Maven Enforcer plugin is used to ensure no dependency version conflicts.
- **Frontend:** npm or yarn pins all packages in `package-lock.json` or `yarn.lock`.
- **Scanning:** Snyk or GitHub Dependabot scans for vulnerabilities in both Java and JavaScript dependencies. High/critical issues block the build pipeline.

---

## 10. Monitoring and Incident Response

### 10.1 Security Event Logging

The following events are logged with timestamp, userId (if authenticated), IP address, and user agent:

- Successful login
- Failed login attempt
- Account lockout triggered
- Password change
- Password reset requested and completed
- Email verification
- Transaction creation and deletion
- Budget threshold alerts triggered
- AI API errors and timeouts

Logs do not contain passwords, tokens, or raw financial amounts.

### 10.2 Anomaly Detection Triggers

The following conditions trigger an automated alert to the system administrator:

- More than 100 failed login attempts from a single IP within 5 minutes
- More than 10 accounts locked within 1 hour
- AI API key usage exceeds a configured daily token limit threshold
- Database connection pool exhaustion
- HTTP 500 error rate exceeds 1% of requests in a 5-minute window

### 10.3 Incident Response Plan

**Step 1 - Detection:** Alert triggered via monitoring system or user report.

**Step 2 - Containment:** Depending on severity, actions include blocking the offending IP, revoking all active tokens for affected users, or taking the service offline.

**Step 3 - Investigation:** Review structured logs to determine the scope and vector of the incident.

**Step 4 - Remediation:** Apply patches, rotate compromised secrets, and restore from backup if data integrity is affected.

**Step 5 - Notification:** Notify affected users within 72 hours of a confirmed data breach, in accordance with applicable data protection regulations.

**Step 6 - Post-Incident Review:** Document the incident, root cause, and corrective actions taken.

---

## 11. Security Checklist

The following checklist must be verified before every production deployment.

**Authentication**
- Passwords validated against strength policy on registration and change
- bcrypt work factor is 12 or higher
- JWT secrets are at least 256 bits and stored as environment variables
- Refresh tokens are hashed before DB storage
- Account lockout is active and tested
- Email verification is required before first login

**API**
- All protected routes require valid JWT
- CORS is restricted to the frontend domain
- All security headers are present and correct
- Rate limiting is active on all endpoints
- Input validation (Zod) is applied on all routes

**Data**
- No sensitive fields (password_hash, tokens) returned in API responses
- All DB queries use parameterized statements
- Aggregated data only is sent to the AI API

**Infrastructure**
- TLS 1.2+ is enforced; older versions disabled
- HTTP redirects to HTTPS
- .env files are in .gitignore
- No secrets in Docker images
- DB is not externally accessible
- npm audit passes with no high/critical issues

**Monitoring**
- Security events are logged
- Alerting thresholds are configured

---

## 12. Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | March 28, 2026 | Project Team | Initial draft |