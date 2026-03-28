# Testing Strategy

## FinTrack AI - Intelligent Personal Finance Management System

**Version:** 1.0  
**Date:** March 28, 2026  
**Status:** Draft  
**Reference:** SRS v1.0, LLD v1.0, API v1.0

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Testing Objectives](#2-testing-objectives)
3. [Testing Scope](#3-testing-scope)
4. [Testing Levels](#4-testing-levels)
5. [Unit Testing](#5-unit-testing)
6. [Integration Testing](#6-integration-testing)
7. [End-to-End Testing](#7-end-to-end-testing)
8. [API Testing](#8-api-testing)
9. [Security Testing](#9-security-testing)
10. [Performance Testing](#10-performance-testing)
11. [Test Data Management](#11-test-data-management)
12. [CI/CD Integration](#12-cicd-integration)
13. [Test Coverage Requirements](#13-test-coverage-requirements)
14. [Defect Management](#14-defect-management)
15. [Test Case Reference](#15-test-case-reference)
16. [Revision History](#16-revision-history)

---

## 1. Introduction

### 1.1 Purpose

This Testing Strategy document defines the approach, scope, levels, tools, and test cases for validating FinTrack AI against the requirements defined in the SRS. It ensures that all functional requirements, non-functional requirements, security controls, and API contracts are verified before deployment.

### 1.2 Scope

This document covers all testing activities for the FinTrack AI backend API, frontend React application, database layer, AI integration, and deployment configuration.

### 1.3 Audience

QA engineers, developers, technical leads, and project managers involved in the development and release of FinTrack AI.

---

## 2. Testing Objectives

- Verify that all functional requirements (FR-1.1 through FR-7.6) are correctly implemented.
- Verify that all non-functional requirements (NFR-1.1 through NFR-6.2) are met.
- Validate that the API behaves according to the API Specification v1.0.
- Confirm that security controls prevent unauthorized access and common attack vectors.
- Ensure the system performs within defined response time thresholds under expected load.
- Detect and document defects early in the development cycle through automated testing.

---

## 3. Testing Scope

### 3.1 In Scope

- Backend API endpoints (all routes defined in API Specification)
- Service layer business logic
- Repository layer database queries
- Middleware (authentication, validation, rate limiting, error handling)
- Frontend component rendering and user interactions
- NLP parsing behavior
- AI analysis result structure validation
- Budget threshold alert triggering
- CSV export correctness
- Security controls: authentication, authorization, input validation
- Performance under load

### 3.2 Out of Scope

- Testing of the AI model's analytical accuracy (treated as a third-party service)
- Testing of the SMTP provider's email delivery (mocked in tests)
- Third-party infrastructure (cloud provider, DNS, CDN)
- Browser compatibility testing beyond the four browsers listed in the SRS

---

## 4. Testing Levels

| Level | Scope | Tool | Responsibility |
|-------|-------|------|----------------|
| Unit Testing (Backend) | Individual Java classes/methods | JUnit 5 + Mockito | Developer |
| Unit Testing (Web) | React components and hooks | Vitest + React Testing Library | Developer |
| Unit Testing (Desktop) | JavaFX controllers and logic | TestFX | Developer |
| Unit Testing (Mobile) | Android view models and logic | JUnit 4/5 + Mockito | Developer |
| Integration Testing | Module interactions, DB queries | Spring Boot Test + Testcontainers | Developer |
| End-to-End Testing (Web) | Full user journeys via browser | Playwright | QA Engineer |
| End-to-End Testing (Mobile)| Native Android flows | Espresso / UI Automator | QA Engineer |
| API Testing | REST contract validation | RestAssured | Developer / QA |
| Security Testing | Auth, injection, headers | OWASP ZAP | QA Engineer |
| Performance Testing | Load and response time | k6 | QA Engineer |

---

## 5. Unit Testing

### 5.1 Framework and Configuration

- **Backend:** JUnit 5, Mockito, AssertJ
  - Location: `backend/src/test/java/**`
  - Execution: `mvn test` or `./gradlew test`
- **Web:** Vitest, React Testing Library
  - Location: `frontend/src/**/*.test.tsx`
  - Execution: `npm run test`
- **Desktop:** TestFX
  - Location: `desktop/src/test/java/**`
- **Mobile:** JUnit, Mockito, Robolectric
  - Location: `mobile/app/src/test/java/**`

### 5.2 What to Unit Test

Unit tests focus on pure business logic within service classes. Repositories and external services are mocked.

**Auth Service - Unit Tests**

| Test ID | Function | Scenario | Expected Outcome |
|---------|----------|----------|-----------------|
| U-AUTH-01 | registerUser | Valid input | Calls hashPassword, createUser, sendVerificationEmail |
| U-AUTH-02 | registerUser | Duplicate email | Throws ConflictError |
| U-AUTH-03 | loginUser | Valid credentials, verified email | Returns accessToken and refreshToken |
| U-AUTH-04 | loginUser | Invalid password | Throws UnauthorizedError |
| U-AUTH-05 | loginUser | Email not verified | Throws ForbiddenError |
| U-AUTH-06 | loginUser | User not found | Throws UnauthorizedError (same error as wrong password) |
| U-AUTH-07 | verifyEmail | Valid unexpired token | Calls markEmailVerified |
| U-AUTH-08 | verifyEmail | Expired token | Throws BadRequestError |
| U-AUTH-09 | requestPasswordReset | Unknown email | Returns generic success without error |
| U-AUTH-10 | resetPassword | Valid token | Updates password, clears token |
| U-AUTH-11 | resetPassword | Expired token | Throws BadRequestError |

**Transaction Service - Unit Tests**

| Test ID | Function | Scenario | Expected Outcome |
|---------|----------|----------|-----------------|
| U-TXN-01 | createTransaction | Valid data, owned category | Calls repository.create, then budget check |
| U-TXN-02 | createTransaction | Category not owned by user | Throws NotFoundError |
| U-TXN-03 | createTransaction | System default category | Succeeds (allowed) |
| U-TXN-04 | getTransactions | All filters applied | Passes correct filter object to repository |
| U-TXN-05 | getTransactions | Default pagination | page=1, limit=20 passed to repository |
| U-TXN-06 | updateTransaction | Transaction not owned | Throws NotFoundError |
| U-TXN-07 | deleteTransaction | Valid owned transaction | Calls repository.delete |
| U-TXN-08 | getDashboardSummary | Current month data | Returns income, expenses, netBalance, recent |

**Budget Service - Unit Tests**

| Test ID | Function | Scenario | Expected Outcome |
|---------|----------|----------|-----------------|
| U-BUD-01 | checkBudgetAfterTransaction | Spend at 85% | Triggers warning alert |
| U-BUD-02 | checkBudgetAfterTransaction | Spend at 100%+ | Triggers exceeded alert |
| U-BUD-03 | checkBudgetAfterTransaction | Spend at 60% | No alert triggered |
| U-BUD-04 | checkBudgetAfterTransaction | No budget for category | Returns without action |
| U-BUD-05 | createBudget | Duplicate period | Throws ConflictError |

**Category Service - Unit Tests**

| Test ID | Function | Scenario | Expected Outcome |
|---------|----------|----------|-----------------|
| U-CAT-01 | deleteCategory | Custom category | Reassigns transactions, then deletes |
| U-CAT-02 | deleteCategory | System default category | Throws ForbiddenError |
| U-CAT-03 | updateCategory | System default category | Throws ForbiddenError |
| U-CAT-04 | createCategory | Duplicate name | Throws ConflictError |

**NLP Service - Unit Tests**

| Test ID | Function | Scenario | Expected Outcome |
|---------|----------|----------|-----------------|
| U-NLP-01 | parseNaturalLanguageInput | Valid text parsed by AI | Returns parsed: true with draft |
| U-NLP-02 | parseNaturalLanguageInput | AI returns malformed JSON | Returns parsed: false |
| U-NLP-03 | parseNaturalLanguageInput | Amount is negative | Returns parsed: false |

**AI Analysis Service - Unit Tests**

| Test ID | Function | Scenario | Expected Outcome |
|---------|----------|----------|-----------------|
| U-AI-01 | getInsights | Fewer than 30 transactions | Returns sufficient: false |
| U-AI-02 | getInsights | Cache hit | Returns cached result without calling AI API |
| U-AI-03 | getInsights | Cache miss | Calls AI API, stores in cache |
| U-AI-04 | getSpendingProjection | 15 days elapsed | Correctly calculates daily rate and projection |
| U-AI-05 | detectAnomalies | Transaction 3x above average | Includes transaction in anomalies list |

### 5.3 Sample Unit Test Structure

```javascript
// budget.service.unit.test.js
describe('BudgetService.checkBudgetAfterTransaction', () => {
  it('should trigger a warning alert when spend reaches 85%', async () => {
    budgetRepository.findByUserCategoryAndPeriod.mockResolvedValue({
      id: 'budget-id',
      limitAmount: 5000,
      category: { name: 'Food' }
    });
    transactionRepository.sumByCategoryAndPeriod.mockResolvedValue(4250);
    notificationService.sendBudgetAlert = jest.fn();

    await budgetService.checkBudgetAfterTransaction('user-id', 'cat-id', '2026-03-15');

    expect(notificationService.sendBudgetAlert).toHaveBeenCalledWith(
      'user-id', 'Food', 'warning', 4250, 5000
    );
  });
});
```

---

## 6. Integration Testing

### 6.1 Setup

Integration tests run using Spring Boot Test and Testcontainers to provide real PostgreSQL and Redis instances.

- Framework: JUnit 5 + Spring Boot Test + Testcontainers
- Location: `backend/src/test/java/**/integration`
- Execution: `mvn test -Pintegration`
- Test database: PostgreSQL (Testcontainers)
- Test cache: Redis (Testcontainers)
- Ollama AI and email are mocked.

### 6.2 Integration Test Cases

**Authentication Flow**

| Test ID | Scenario | Steps | Expected Outcome |
|---------|----------|-------|-----------------|
| I-AUTH-01 | Full registration and login | Register, verify email, login | Returns valid JWT |
| I-AUTH-02 | Login before email verification | Register, login without verifying | 403 response |
| I-AUTH-03 | Password reset flow | Request reset, use token, login with new password | Login succeeds |
| I-AUTH-04 | Expired reset token | Use a token older than 1 hour | 400 response |
| I-AUTH-05 | Refresh token rotation | Login, refresh, use old refresh token | 401 on second use of old token |

**Transaction CRUD**

| Test ID | Scenario | Expected Outcome |
|---------|----------|-----------------|
| I-TXN-01 | Create transaction with valid data | 201, transaction in DB |
| I-TXN-02 | Get transactions with date filter | Returns only transactions in range |
| I-TXN-03 | Search transactions by keyword | Returns matching transactions |
| I-TXN-04 | Delete transaction | 204, transaction removed from DB |
| I-TXN-05 | Access other user's transaction | 404 |

**Budget and Alert Integration**

| Test ID | Scenario | Expected Outcome |
|---------|----------|-----------------|
| I-BUD-01 | Create transaction that triggers budget warning | Notification created in DB |
| I-BUD-02 | Create transaction that triggers budget exceeded | Notification type is "budget_exceeded" |
| I-BUD-03 | Create duplicate budget | 409 response |

**Category Deletion Cascade**

| Test ID | Scenario | Expected Outcome |
|---------|----------|-----------------|
| I-CAT-01 | Delete custom category with transactions | Transactions reassigned to Uncategorized |
| I-CAT-02 | Attempt to delete system category | 403 response |

---

## 7. End-to-End Testing

### 7.1 Framework and Setup

- Framework: Playwright
- Location: `frontend/e2e/*.spec.js`
- Execution: `npm run test:e2e`
- Target: Staging environment running full stack
- Browser: Chromium (primary), Firefox (secondary)

### 7.2 E2E Test Scenarios

**Scenario: New User Onboarding**

```
1. Navigate to /register
2. Fill in name, email, password
3. Submit form
4. Verify success message displayed
5. Check email (intercepted in test) for verification link
6. Follow verification link
7. Navigate to /login
8. Log in with credentials
9. Assert redirect to dashboard
10. Assert dashboard shows Rs. 0 income, Rs. 0 expenses
```

**Scenario: Add Transaction and View Dashboard**

```
1. Log in as existing user
2. Click "Add Transaction"
3. Fill in: amount = 1500, type = expense, category = Food, date = today
4. Submit
5. Assert transaction appears in recent transactions list on dashboard
6. Assert "Total Expenses" card shows Rs. 1,500
```

**Scenario: Natural Language Transaction Entry**

```
1. Log in
2. Navigate to Transactions
3. Click "Add with text"
4. Type: "Spent 300 on transportation"
5. Assert confirmation screen shows amount = 300, category = Transportation
6. Click Confirm
7. Assert transaction appears in list
```

**Scenario: Budget Alert Display**

```
1. Log in as user with Food budget of Rs. 1,000
2. Add an expense of Rs. 850 in Food category
3. Assert in-app notification appears: "Budget Warning: Food"
4. Navigate to Budgets page
5. Assert Food budget shows "warning" status and 85% progress
```

**Scenario: Reports and CSV Export**

```
1. Log in as user with multiple months of transactions
2. Navigate to Reports
3. Assert pie chart renders with categories
4. Assert bar chart shows 6 months of data
5. Set date range to current month
6. Click Export CSV
7. Assert file download initiates
8. Verify CSV contains correct headers: Date, Type, Category, Description, Amount
```

**Scenario: Unauthorized Access Redirect**

```
1. Navigate to /dashboard without logging in
2. Assert redirect to /login
3. Assert login page is displayed
```

---

## 8. API Testing

### 8.1 Contract Testing

API contract tests verify that every endpoint returns the exact response shape defined in the API Specification v1.0.

- Framework: Jest + Supertest
- Location: `backend/__tests__/api/*.api.test.js`
- Execution: `npm run test:api`

### 8.2 Key API Test Cases

**POST /auth/register**

| Test ID | Input | Expected Code | Expected Response |
|---------|-------|--------------|-------------------|
| A-REG-01 | Valid payload | 201 | success: true, data.message present |
| A-REG-02 | Missing name | 422 | fields array includes "name" |
| A-REG-03 | Invalid email | 422 | fields array includes "email" |
| A-REG-04 | Weak password | 422 | fields array includes "password" |
| A-REG-05 | Duplicate email | 409 | error.code: 409 |

**POST /auth/login**

| Test ID | Input | Expected Code | Expected Response |
|---------|-------|--------------|-------------------|
| A-LOGIN-01 | Valid credentials | 200 | accessToken, refreshToken, user object |
| A-LOGIN-02 | Wrong password | 401 | error.code: 401 |
| A-LOGIN-03 | Unknown email | 401 | Same error as wrong password |

**POST /transactions**

| Test ID | Input | Expected Code |
|---------|-------|--------------|
| A-TXN-01 | Valid, authenticated | 201 |
| A-TXN-02 | No auth token | 401 |
| A-TXN-03 | Negative amount | 422 |
| A-TXN-04 | Future date | 422 |
| A-TXN-05 | Invalid category UUID | 422 |
| A-TXN-06 | Category not owned | 404 |

**GET /transactions**

| Test ID | Input | Expected Code |
|---------|-------|--------------|
| A-TXN-07 | Authenticated, no filters | 200, pagination object present |
| A-TXN-08 | limit=200 (exceeds max) | 422 |
| A-TXN-09 | Invalid from date format | 422 |

**GET /analysis/insights**

| Test ID | Scenario | Expected Code |
|---------|----------|--------------|
| A-AI-01 | User has 30+ transactions | 200, sufficient: true |
| A-AI-02 | User has fewer than 30 transactions | 200, sufficient: false |
| A-AI-03 | No auth token | 401 |

---

## 9. Security Testing

### 9.1 Authentication and Authorization Tests

| Test ID | Attack / Scenario | Expected Result |
|---------|------------------|-----------------|
| S-01 | Access protected endpoint without token | 401 |
| S-02 | Access protected endpoint with expired token | 401 |
| S-03 | Access protected endpoint with tampered JWT payload | 401 |
| S-04 | Access another user's transaction by guessing UUID | 404 |
| S-05 | Reuse a refresh token after rotation | 401 |
| S-06 | Delete a system-default category | 403 |

### 9.2 Input Injection Tests

| Test ID | Attack | Endpoint | Expected Result |
|---------|--------|----------|-----------------|
| S-07 | SQL injection in description: `' OR '1'='1` | POST /transactions | 201 (stored as text), no DB error |
| S-08 | SQL injection in search param: `' OR '1'='1` | GET /transactions | 200, no extra results |
| S-09 | XSS payload in description: `<script>alert(1)</script>` | POST /transactions | Stored as text, rendered escaped in frontend |
| S-10 | Oversized description (>255 chars) | POST /transactions | 422 |
| S-11 | Negative amount | POST /transactions | 422 |
| S-12 | Extremely large amount (>NUMERIC max) | POST /transactions | 422 |

### 9.3 Header and Transport Tests

| Test ID | Scenario | Expected Result |
|---------|----------|-----------------|
| S-13 | HTTP request to API domain | 301 redirect to HTTPS |
| S-14 | Check HSTS header on HTTPS response | Strict-Transport-Security header present |
| S-15 | Check X-Content-Type-Options header | nosniff value present |
| S-16 | Check X-Frame-Options header | DENY value present |
| S-17 | CORS request from unauthorized origin | 403 or CORS headers absent |

### 9.4 Rate Limiting Tests

| Test ID | Scenario | Expected Result |
|---------|----------|-----------------|
| S-18 | 11 POST /auth/login requests in 10 minutes from same IP | 11th request returns 429 |
| S-19 | 301 protected API requests in 15 minutes | 301st returns 429 with Retry-After header |

### 9.5 Automated Security Scan

OWASP ZAP is run against the staging environment before each release:

```bash
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t https://staging.fintrack.ai \
  -r zap_report.html
```

All High and Medium severity alerts must be resolved before a production release.

---

## 10. Performance Testing

### 10.1 Tool

k6 (open-source load testing tool) is used for performance and load testing.

### 10.2 Performance Test Scenarios

**Scenario 1: Baseline API Response Time**

Target: Dashboard endpoint loads within 3 seconds under normal conditions.

```javascript
// k6 script: baseline.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 10,
  duration: '60s',
};

export default function () {
  const res = http.get('https://staging.api.fintrack.ai/api/v1/dashboard', {
    headers: { Authorization: `Bearer ${__ENV.TEST_TOKEN}` },
  });
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 3000ms': (r) => r.timings.duration < 3000,
  });
}
```

**Scenario 2: Concurrent User Load**

Target: 500 concurrent users without degradation (per NFR-1.4).

```javascript
export const options = {
  stages: [
    { duration: '2m', target: 100 },
    { duration: '5m', target: 500 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.01'],
  },
};
```

**Scenario 3: Transaction Search Performance**

Target: Search results within 2 seconds for 10,000 transaction records (per NFR-1.2).

The test seeds 10,000 transactions for a test user and runs repeated search queries with varying filters.

### 10.3 Acceptance Criteria

| Metric | Threshold |
|--------|-----------|
| Dashboard load time (p95) | Under 3 seconds |
| Transaction list with filters (p95) | Under 2 seconds |
| AI insights endpoint (p95) | Under 5 seconds |
| Error rate under 500 VU load | Below 1% |
| Requests per second at 500 VUs | Baseline established and maintained |

---

## 11. Test Data Management

### 11.1 Test Data Strategy

- Unit tests: all data is created inline within the test using mock factories.
- Integration tests: a dedicated test database is seeded before each suite using Prisma's seed script adapted for testing. All data is deleted after each suite.
- E2E tests: use a dedicated test user account on the staging environment. Test data is created and cleaned up as part of the test lifecycle using API calls in beforeAll and afterAll hooks.
- Performance tests: a separate test dataset of 10,000 transactions is seeded once before the performance test run.

### 11.2 Test User Accounts

The following test accounts are pre-seeded on the staging environment:

| Account | Purpose |
|---------|---------|
| e2e_user@test.fintrack.ai | E2E tests for standard user flows |
| budget_user@test.fintrack.ai | E2E tests for budget and alert flows |
| perf_user@test.fintrack.ai | Performance testing with large dataset |

Passwords for test accounts are stored in the team's secrets manager.

### 11.3 Sensitive Data in Tests

- Real user data is never used in any test environment.
- All test financial figures are clearly artificial (round numbers like 1000, 5000).
- Email verification in tests is handled by intercepting outbound emails using a test SMTP server (Mailhog or equivalent) on the staging environment.

---

## 12. CI/CD Integration

All test suites are integrated into the GitHub Actions pipeline.

### 12.1 Pipeline Test Gates

| Stage | Tests Run | Failure Action |
|-------|-----------|---------------|
| On every pull request | Unit tests, API contract tests, npm audit | Block merge |
| On merge to main | Unit tests, integration tests, security scan (ZAP) | Block deployment |
| On staging deployment | E2E tests | Notify team, block production deployment |
| Weekly scheduled | Performance tests | Generate report, notify team |

### 12.2 Test Execution Commands

```bash
# Backend tests (Unit + Integration)
mvn test

# Frontend (Web) tests
npm run test

# Mobile (Android) tests
./gradlew test

# Desktop (JavaFX) tests
mvn test -f desktop/pom.xml

# E2E (Web) tests
npx playwright test

# Performance tests
k6 run tests/performance/baseline.js
```

### 12.3 Coverage Report

Jest collects coverage reports on every CI run. Reports are uploaded to the CI artifacts for review.

---

## 13. Test Coverage Requirements

| Module | Minimum Line Coverage | Minimum Branch Coverage |
|--------|--------------------|------------------------|
| Auth Service | 90% | 85% |
| Transaction Service | 90% | 85% |
| Budget Service | 90% | 85% |
| Category Service | 85% | 80% |
| NLP Service | 80% | 75% |
| AI Analysis Service | 80% | 75% |
| Reporting Engine | 85% | 80% |
| Middleware | 90% | 85% |
| Overall Backend | 85% | 80% |

Coverage below the minimum threshold fails the CI pipeline.

---

## 14. Defect Management

### 14.1 Severity Levels

| Severity | Definition | Resolution Target |
|----------|-----------|-----------------|
| Critical | System down, data loss, authentication bypass | Same day |
| High | Core feature unusable, incorrect financial calculation | Within 2 business days |
| Medium | Feature degraded but workaround exists | Within 1 sprint |
| Low | Minor UI issue, cosmetic defect | Next planned release |

### 14.2 Defect Lifecycle

```
Reported -> Triaged (severity assigned) -> Assigned to Developer
-> In Development -> Fixed -> Verification Testing -> Closed
                                         |
                                         v (if test fails)
                                    Reopened
```

### 14.3 Defect Reporting Template

Each defect report must include:

- Title: short description of the issue
- Severity: Critical / High / Medium / Low
- Environment: Development / Staging / Production
- Steps to Reproduce: numbered list
- Expected Result: what should happen
- Actual Result: what is happening
- Evidence: screenshot, log output, or API response
- Related Requirement: FR or NFR reference from SRS

---

## 15. Test Case Reference

### 15.1 Functional Requirement Coverage

| SRS Requirement | Unit Test IDs | Integration Test IDs | E2E Test IDs |
|-----------------|--------------|---------------------|--------------|
| FR-1.1 Register | U-AUTH-01, U-AUTH-02 | I-AUTH-01 | E2E-01 |
| FR-1.4 Login | U-AUTH-03 through U-AUTH-06 | I-AUTH-01 | E2E-01 |
| FR-1.5 JWT issuance | U-AUTH-03 | I-AUTH-01 | - |
| FR-1.6 Password reset | U-AUTH-09, U-AUTH-10, U-AUTH-11 | I-AUTH-03, I-AUTH-04 | - |
| FR-2.1 Create transaction | U-TXN-01, U-TXN-02, U-TXN-03 | I-TXN-01 | E2E-02 |
| FR-2.4 Paginated list | U-TXN-04, U-TXN-05 | I-TXN-02 | - |
| FR-2.5 Filter transactions | U-TXN-04 | I-TXN-02 | - |
| FR-2.6 Search transactions | - | I-TXN-03 | - |
| FR-3.3 Delete custom category | U-CAT-02 | I-CAT-02 | - |
| FR-3.4 Reassign on delete | U-CAT-01 | I-CAT-01 | - |
| FR-4.1 NLP input | U-NLP-01 through U-NLP-03 | - | E2E-03 |
| FR-5.2 Anomaly detection | U-AI-05 | - | - |
| FR-6.3 Budget warning alert | U-BUD-01 | I-BUD-01 | E2E-04 |
| FR-6.4 Budget exceeded alert | U-BUD-02 | I-BUD-02 | E2E-04 |
| FR-7.1 Pie chart data | - | - | E2E-05 |
| FR-7.5 CSV export | - | - | E2E-05 |

### 15.2 Non-Functional Requirement Coverage

| SRS Requirement | Test Type | Test ID |
|-----------------|-----------|---------|
| NFR-1.1 Dashboard load < 3s | Performance | PERF-01 |
| NFR-1.2 Search < 2s at 10k records | Performance | PERF-03 |
| NFR-1.3 AI insights < 5s | Performance | PERF-01 |
| NFR-1.4 500 concurrent users | Performance | PERF-02 |
| NFR-2.1 TLS enforcement | Security | S-13 |
| NFR-2.2 bcrypt storage | Unit | U-AUTH-01 |
| NFR-2.3 XSS, SQL injection | Security | S-07 through S-12 |
| NFR-2.4 Session expiry | Integration | I-AUTH-05 |
| NFR-2.5 Account lockout | Security | S-18 |

---

## 16. Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | March 28, 2026 | Project Team | Initial draft |