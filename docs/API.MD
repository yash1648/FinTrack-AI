# API Specification

## FinTrack AI - Intelligent Personal Finance Management System

**Version:** 1.0  
**Date:** March 28, 2026  
**Status:** Draft  
**Base URL:** `https://api.fintrack.ai/api/v1`  
**Reference:** SRS v1.0, LLD v1.0

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [General Conventions](#2-general-conventions)
3. [Authentication](#3-authentication)
4. [Auth Endpoints](#4-auth-endpoints)
5. [Transaction Endpoints](#5-transaction-endpoints)
6. [Category Endpoints](#6-category-endpoints)
7. [Budget Endpoints](#7-budget-endpoints)
8. [NLP Endpoints](#8-nlp-endpoints)
9. [Analysis Endpoints](#9-analysis-endpoints)
10. [Report Endpoints](#10-report-endpoints)
11. [Notification Endpoints](#11-notification-endpoints)
12. [Dashboard Endpoint](#12-dashboard-endpoint)
13. [WebSocket Realtime Updates](#13-websocket-realtime-updates)
14. [Error Reference](#14-error-reference)
15. [Rate Limiting](#15-rate-limiting)
16. [Revision History](#16-revision-history)

---

## 1. Introduction

This document specifies all REST API endpoints exposed by the FinTrack AI backend. Each endpoint includes the HTTP method, path, authentication requirement, request parameters, request body schema, and response schema. This document is the contract between the frontend and backend teams.

---

## 2. General Conventions

### 2.1 Request Format

- All request bodies use JSON with Content-Type: application/json
- Date values use ISO 8601 format: YYYY-MM-DD
- Timestamp values use RFC 3339 format: YYYY-MM-DDTHH:MM:SSZ
- Amount values are numbers with up to 2 decimal places
- UUIDs follow the format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

### 2.2 Response Format

All responses return a consistent JSON envelope:

**Success Response:**
```json
{
  "success": true,
  "data": { ... }
}
```

**Paginated Success Response:**
```json
{
  "success": true,
  "data": [ ... ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 150,
    "totalPages": 8
  }
}
```

**Error Response:**
```json
{
  "success": false,
  "error": {
    "code": 400,
    "message": "Descriptive error message"
  }
}
```

### 2.3 HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request succeeded |
| 201 | Created - Resource created successfully |
| 204 | No Content - Delete succeeded |
| 400 | Bad Request - Malformed request |
| 401 | Unauthorized - Missing or invalid token |
| 403 | Forbidden - Authenticated but not permitted |
| 404 | Not Found - Resource does not exist |
| 409 | Conflict - Duplicate resource |
| 422 | Unprocessable Entity - Validation failure |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error |

### 2.4 Authentication Requirement

Endpoints marked **Protected** require the following header:

```
Authorization: Bearer <access_token>
```

---

## 3. Authentication

### Token Lifecycle

- Access tokens expire after 1 hour.
- Refresh tokens expire after 7 days.
- On 401 response, the client should call POST /auth/refresh with the refresh token to obtain a new access token.
- Refresh tokens are rotated on each use.

---

## 4. Auth Endpoints

### POST /auth/register

Register a new user account.

**Access:** Public

**Request Body:**
```json
{
  "name": "Rahul Sharma",
  "email": "rahul@example.com",
  "password": "SecurePass@123"
}
```

**Validation Rules:**
- name: required, 2-100 characters
- email: required, valid email format
- password: required, min 8 characters, must contain uppercase, digit, and special character

**Response 201:**
```json
{
  "success": true,
  "data": {
    "message": "Registration successful. Please verify your email."
  }
}
```

**Errors:** 400 (validation), 409 (email already registered)

---

### GET /auth/verify-email

Verify a user's email address using the token sent in the verification email.

**Access:** Public

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| token | string | Yes | Email verification token |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "message": "Email verified successfully."
  }
}
```

**Errors:** 400 (invalid or expired token)

---

### POST /auth/login

Authenticate a user and obtain tokens.

**Access:** Public

**Request Body:**
```json
{
  "email": "rahul@example.com",
  "password": "SecurePass@123"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "user": {
      "id": "uuid",
      "name": "Rahul Sharma",
      "email": "rahul@example.com",
      "currency": "INR"
    }
  }
}
```

**Errors:** 400 (validation), 401 (invalid credentials), 403 (email not verified)

---

### POST /auth/refresh

Obtain a new access token using a refresh token.

**Access:** Public

**Request Body:**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Errors:** 401 (invalid or expired refresh token)

---

### POST /auth/logout

Invalidate the current refresh token.

**Access:** Protected

**Request Body:**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "message": "Logged out successfully."
  }
}
```

---

### POST /auth/forgot-password

Request a password reset link.

**Access:** Public

**Request Body:**
```json
{
  "email": "rahul@example.com"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "message": "If your email is registered, a reset link has been sent."
  }
}
```

Note: Always returns 200 to prevent email enumeration.

---

### POST /auth/reset-password

Reset a user's password using the reset token.

**Access:** Public

**Request Body:**
```json
{
  "token": "resettoken123abc",
  "newPassword": "NewSecurePass@456"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "message": "Password reset successfully."
  }
}
```

**Errors:** 400 (invalid or expired token), 422 (password validation)

---

### GET /auth/profile

Get the authenticated user's profile.

**Access:** Protected

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Rahul Sharma",
    "email": "rahul@example.com",
    "currency": "INR",
    "createdAt": "2026-01-15T10:00:00Z"
  }
}
```

---

### PATCH /auth/profile

Update the authenticated user's profile.

**Access:** Protected

**Request Body (all fields optional):**
```json
{
  "name": "Rahul R. Sharma",
  "currency": "INR"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Rahul R. Sharma",
    "email": "rahul@example.com",
    "currency": "INR"
  }
}
```

---

### PATCH /auth/change-password

Change the authenticated user's password.

**Access:** Protected

**Request Body:**
```json
{
  "currentPassword": "SecurePass@123",
  "newPassword": "NewSecurePass@456"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "message": "Password changed successfully."
  }
}
```

**Errors:** 401 (current password incorrect), 422 (new password validation)

---

## 5. Transaction Endpoints

### POST /transactions

Create a new transaction.

**Access:** Protected

**Request Body:**
```json
{
  "amount": 500.00,
  "type": "expense",
  "category_id": "uuid",
  "description": "Grocery shopping at D-Mart",
  "date": "2026-03-15"
}
```

**Validation Rules:**
- amount: positive number, max 2 decimal places, required
- type: "income" or "expense", required
- category_id: valid UUID, must belong to user or be system default, required
- description: max 255 characters, optional
- date: valid date, not in the future, required

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "amount": 500.00,
    "type": "expense",
    "category": {
      "id": "uuid",
      "name": "Food"
    },
    "description": "Grocery shopping at D-Mart",
    "date": "2026-03-15",
    "createdAt": "2026-03-15T14:30:00Z"
  }
}
```

**Errors:** 400, 404 (category not found), 422

---

### GET /transactions

Get a paginated and optionally filtered list of transactions.

**Access:** Protected

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | integer | No | 1 | Page number |
| limit | integer | No | 20 | Items per page, max 100 |
| from | date | No | - | Start date (YYYY-MM-DD) |
| to | date | No | - | End date (YYYY-MM-DD) |
| type | string | No | - | "income" or "expense" |
| category_id | UUID | No | - | Filter by category |
| search | string | No | - | Keyword search on description |
| min_amount | number | No | - | Minimum amount filter |
| max_amount | number | No | - | Maximum amount filter |

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "amount": 500.00,
      "type": "expense",
      "category": { "id": "uuid", "name": "Food" },
      "description": "Grocery shopping at D-Mart",
      "date": "2026-03-15",
      "createdAt": "2026-03-15T14:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 142,
    "totalPages": 8
  }
}
```

---

### GET /transactions/:id

Get a single transaction by ID.

**Access:** Protected

**Path Parameters:** id (UUID)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "amount": 500.00,
    "type": "expense",
    "category": { "id": "uuid", "name": "Food" },
    "description": "Grocery shopping at D-Mart",
    "date": "2026-03-15",
    "createdAt": "2026-03-15T14:30:00Z",
    "updatedAt": "2026-03-15T14:30:00Z"
  }
}
```

**Errors:** 404

---

### PATCH /transactions/:id

Update an existing transaction.

**Access:** Protected

**Path Parameters:** id (UUID)

**Request Body (all fields optional):**
```json
{
  "amount": 450.00,
  "category_id": "uuid",
  "description": "Updated description",
  "date": "2026-03-14"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "amount": 450.00,
    "type": "expense",
    "category": { "id": "uuid", "name": "Food" },
    "description": "Updated description",
    "date": "2026-03-14",
    "updatedAt": "2026-03-15T15:00:00Z"
  }
}
```

**Errors:** 400, 404, 422

---

### DELETE /transactions/:id

Delete a transaction.

**Access:** Protected

**Path Parameters:** id (UUID)

**Response 204:** No body

**Errors:** 404

---

## 6. Category Endpoints

### GET /categories

Get all categories available to the authenticated user (system defaults and user-created).

**Access:** Protected

**Response 200:**
```json
{
  "success": true,
  "data": [
    { "id": "uuid", "name": "Food", "isDefault": true },
    { "id": "uuid", "name": "Transportation", "isDefault": true },
    { "id": "uuid", "name": "My Custom Category", "isDefault": false }
  ]
}
```

---

### POST /categories

Create a new custom category.

**Access:** Protected

**Request Body:**
```json
{
  "name": "Pet Expenses"
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Pet Expenses",
    "isDefault": false,
    "createdAt": "2026-03-15T10:00:00Z"
  }
}
```

**Errors:** 409 (name already exists for this user), 422

---

### PATCH /categories/:id

Rename a custom category.

**Access:** Protected

**Path Parameters:** id (UUID)

**Request Body:**
```json
{
  "name": "Pets and Animals"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Pets and Animals",
    "isDefault": false
  }
}
```

**Errors:** 403 (cannot modify system category), 404, 409

---

### DELETE /categories/:id

Delete a custom category. Transactions under it are reassigned to "Uncategorized".

**Access:** Protected

**Path Parameters:** id (UUID)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "message": "Category deleted. Transactions reassigned to Uncategorized."
  }
}
```

**Errors:** 403 (cannot delete system category), 404

---

## 7. Budget Endpoints

### GET /budgets

Get all budgets for the current month with spending progress.

**Access:** Protected

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| month | integer | No | Month (1-12), defaults to current |
| year | integer | No | Year, defaults to current |

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "category": { "id": "uuid", "name": "Food" },
      "limitAmount": 5000.00,
      "spent": 3850.00,
      "remaining": 1150.00,
      "percentage": 77.0,
      "status": "warning",
      "month": 3,
      "year": 2026
    }
  ]
}
```

Status values: "ok" (below 80%), "warning" (80-99%), "exceeded" (100%+)

---

### POST /budgets

Create a new budget.

**Access:** Protected

**Request Body:**
```json
{
  "category_id": "uuid",
  "limit_amount": 5000.00,
  "month": 3,
  "year": 2026
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "category": { "id": "uuid", "name": "Food" },
    "limitAmount": 5000.00,
    "month": 3,
    "year": 2026,
    "createdAt": "2026-03-01T10:00:00Z"
  }
}
```

**Errors:** 409 (budget already exists for this category/period), 422

---

### PATCH /budgets/:id

Update an existing budget limit.

**Access:** Protected

**Path Parameters:** id (UUID)

**Request Body:**
```json
{
  "limit_amount": 6000.00
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "limitAmount": 6000.00,
    "updatedAt": "2026-03-15T10:00:00Z"
  }
}
```

**Errors:** 404, 422

---

### DELETE /budgets/:id

Delete a budget.

**Access:** Protected

**Path Parameters:** id (UUID)

**Response 204:** No body

**Errors:** 404

---

## 8. NLP Endpoints

### POST /nlp/parse

Parse a natural language string into a draft transaction.

**Access:** Protected

**Request Body:**
```json
{
  "text": "Spent 500 rupees on groceries today"
}
```

**Response 200 (successful parse):**
```json
{
  "success": true,
  "data": {
    "parsed": true,
    "draft": {
      "amount": 500.00,
      "type": "expense",
      "category_id": "uuid",
      "category_name": "Food",
      "description": "groceries",
      "date": "2026-03-28"
    }
  }
}
```

**Response 200 (unable to parse):**
```json
{
  "success": true,
  "data": {
    "parsed": false,
    "message": "Could not extract transaction details. Please use the manual entry form."
  }
}
```

Note: This endpoint always returns 200. The parsed flag indicates whether extraction succeeded. The draft is confirmed by the user before being submitted to POST /transactions.

---

## 9. Analysis Endpoints

### GET /analysis/insights

Get AI-generated financial insights for the authenticated user.

**Access:** Protected

**Response 200 (sufficient data):**
```json
{
  "success": true,
  "data": {
    "sufficient": true,
    "cachedAt": "2026-03-28T08:00:00Z",
    "patterns": [
      "Your food spending has increased by 23% compared to last month.",
      "You consistently spend the most on weekends."
    ],
    "anomalies": [
      {
        "transactionId": "uuid",
        "date": "2026-03-10",
        "amount": 4500.00,
        "category": "Entertainment",
        "reason": "This is 3.2x your average entertainment spend."
      }
    ],
    "recommendations": [
      "Consider reducing dining out expenses. You have spent Rs. 3,200 on food this month.",
      "Your utility bills are consistent. No action needed."
    ],
    "projectedMonthlyExpense": 18500.00
  }
}
```

**Response 200 (insufficient data):**
```json
{
  "success": true,
  "data": {
    "sufficient": false,
    "message": "Add at least 30 days of transactions to unlock AI insights."
  }
}
```

---

### GET /analysis/anomalies

Get a list of anomalous transactions detected in the last 90 days.

**Access:** Protected

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "transactionId": "uuid",
      "date": "2026-03-10",
      "amount": 4500.00,
      "category": "Entertainment",
      "average": 1400.00,
      "deviation": "3.2x above average"
    }
  ]
}
```

---

### GET /analysis/projection

Get the projected total expense for the current month.

**Access:** Protected

**Response 200:**
```json
{
  "success": true,
  "data": {
    "spent": 9250.00,
    "projected": 18750.00,
    "daysElapsed": 15,
    "daysInMonth": 31,
    "currency": "INR"
  }
}
```

---

## 10. Report Endpoints

### GET /reports/category-distribution

Get expense breakdown by category for a given date range.

**Access:** Protected

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| from | date | Yes | Start date |
| to | date | Yes | End date |

**Response 200:**
```json
{
  "success": true,
  "data": [
    { "categoryId": "uuid", "categoryName": "Food", "total": 8500.00, "percentage": 42.3 },
    { "categoryId": "uuid", "categoryName": "Transportation", "total": 3200.00, "percentage": 15.9 },
    { "categoryId": "uuid", "categoryName": "Entertainment", "total": 2800.00, "percentage": 13.9 }
  ]
}
```

---

### GET /reports/monthly-comparison

Get income and expense totals for the last 6 months.

**Access:** Protected

**Response 200:**
```json
{
  "success": true,
  "data": [
    { "month": "Oct 2025", "income": 50000.00, "expenses": 32000.00 },
    { "month": "Nov 2025", "income": 50000.00, "expenses": 28500.00 },
    { "month": "Dec 2025", "income": 55000.00, "expenses": 41000.00 },
    { "month": "Jan 2026", "income": 50000.00, "expenses": 30000.00 },
    { "month": "Feb 2026", "income": 50000.00, "expenses": 27000.00 },
    { "month": "Mar 2026", "income": 50000.00, "expenses": 18500.00 }
  ]
}
```

---

### GET /reports/daily-spending

Get cumulative daily expense data for a specific month.

**Access:** Protected

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| year | integer | Yes | Year (e.g. 2026) |
| month | integer | Yes | Month (1-12) |

**Response 200:**
```json
{
  "success": true,
  "data": [
    { "date": "2026-03-01", "daily": 850.00, "cumulative": 850.00 },
    { "date": "2026-03-02", "daily": 0.00, "cumulative": 850.00 },
    { "date": "2026-03-03", "daily": 1200.00, "cumulative": 2050.00 }
  ]
}
```

---

### GET /reports/summary

Get summary statistics for a given date range.

**Access:** Protected

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| from | date | Yes | Start date |
| to | date | Yes | End date |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "totalIncome": 50000.00,
    "totalExpenses": 18500.00,
    "netBalance": 31500.00,
    "largestExpense": {
      "amount": 4500.00,
      "description": "Electronics purchase",
      "date": "2026-03-10"
    },
    "transactionCount": 47,
    "averageDailySpend": 616.67,
    "currency": "INR"
  }
}
```

---

### GET /reports/export

Export transactions as a CSV file for a given date range.

**Access:** Protected

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| from | date | Yes | Start date |
| to | date | Yes | End date |

**Response 200:**
- Content-Type: text/csv
- Content-Disposition: attachment; filename="transactions_2026-03-01_2026-03-28.csv"

```
Date,Type,Category,Description,Amount
2026-03-28,expense,Food,Grocery shopping,500.00
2026-03-27,income,Salary,Monthly salary,50000.00
```

---

## 11. Notification Endpoints

### GET /notifications

Get the authenticated user's notifications.

**Access:** Protected

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| unread_only | boolean | No | false | If true, returns only unread notifications |
| page | integer | No | 1 | Page number |
| limit | integer | No | 20 | Items per page |

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "type": "budget_warning",
      "title": "Budget Warning: Food",
      "body": "You have used 85% of your Food budget for March 2026.",
      "isRead": false,
      "createdAt": "2026-03-28T14:00:00Z"
    }
  ],
  "pagination": { "page": 1, "limit": 20, "total": 3, "totalPages": 1 }
}
```

---

### PATCH /notifications/:id/read

Mark a specific notification as read.

**Access:** Protected

**Path Parameters:** id (UUID)

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "Notification marked as read." }
}
```

---

### PATCH /notifications/read-all

Mark all notifications as read.

**Access:** Protected

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "All notifications marked as read." }
}
```

---

## 12. Dashboard Endpoint

### GET /dashboard

Get the current month's financial summary for the dashboard view.

**Access:** Protected

**Response 200:**
```json
{
  "success": true,
  "data": {
    "totalIncome": 50000.00,
    "totalExpenses": 18500.00,
    "netBalance": 31500.00,
    "currency": "INR",
    "month": "March 2026",
    "recentTransactions": [
      {
        "id": "uuid",
        "amount": 500.00,
        "type": "expense",
        "category": { "id": "uuid", "name": "Food" },
        "description": "Grocery shopping",
        "date": "2026-03-28"
      }
    ],
    "activeBudgetAlerts": [
      {
        "budgetId": "uuid",
        "categoryName": "Food",
        "status": "warning",
        "percentage": 85.0
      }
    ]
  }
}
```

---

## 13. WebSocket Realtime Updates

The FinTrack AI system uses WebSockets for real-time notifications and budget alerts.

**Endpoint:** `ws://api.fintrack.ai/ws`

**Authentication:** 
Clients must provide the access token as a query parameter or in the initial handshake headers.

`ws://api.fintrack.ai/ws?token=<access_token>`

### Server-to-Client Messages

The server pushes JSON messages to clients.

#### 13.1 Budget Alert
Pushed when a transaction causes a budget to reach a warning or exceeded threshold.

```json
{
  "type": "BUDGET_ALERT",
  "payload": {
    "budgetId": "uuid",
    "categoryName": "Food",
    "status": "warning",
    "percentage": 85.0,
    "message": "You have used 85% of your Food budget."
  }
}
```

#### 13.2 Insight Ready
Pushed when AI-generated financial insights have been updated.

```json
{
  "type": "INSIGHT_READY",
  "payload": {
    "message": "New financial insights are available for review."
  }
}
```

#### 13.3 Sync Transaction
Pushed when a transaction is added or updated (e.g. via mobile) to sync the desktop or web view.

```json
{
  "type": "SYNC_TRANSACTION",
  "payload": {
    "action": "CREATE",
    "transactionId": "uuid"
  }
}
```

---

## 14. Error Reference

| HTTP Code | Error Code | Description |
|-----------|-----------|-------------|
| 400 | BAD_REQUEST | Malformed request body or query parameters |
| 401 | UNAUTHORIZED | Missing, invalid, or expired access token |
| 403 | FORBIDDEN | Action not permitted for authenticated user |
| 404 | NOT_FOUND | Requested resource does not exist |
| 409 | CONFLICT | Resource already exists (duplicate) |
| 422 | VALIDATION_ERROR | Input failed field validation rules |
| 429 | RATE_LIMITED | Too many requests; retry after indicated time |
| 500 | INTERNAL_ERROR | Unexpected server error |

**Validation Error Response (422) includes field-level detail:**
```json
{
  "success": false,
  "error": {
    "code": 422,
    "message": "Validation failed",
    "fields": [
      { "field": "amount", "message": "Amount must be a positive number" },
      { "field": "date", "message": "Date cannot be in the future" }
    ]
  }
}
```

---

## 15. Rate Limiting

Rate limits are applied per IP address for public endpoints and per user for protected endpoints.

| Endpoint Group | Limit | Window |
|---------------|-------|--------|
| POST /auth/login | 10 requests | 10 minutes |
| POST /auth/register | 5 requests | 1 hour |
| POST /auth/forgot-password | 5 requests | 1 hour |
| POST /nlp/parse | 30 requests | 1 hour |
| GET /analysis/insights | 20 requests | 1 hour |
| All other protected endpoints | 300 requests | 15 minutes |

When a rate limit is exceeded, the API responds with HTTP 429 and includes a Retry-After header indicating seconds until the limit resets.

---

## 16. Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | March 28, 2026 | Project Team | Initial draft |