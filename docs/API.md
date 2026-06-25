# FinTrack-AI Backend API Documentation

**Base URL:** `http://localhost:8080/api/v1`  
**Framework:** Spring Boot 4.0.5 (Java 21)  
**Authentication:** JWT Bearer Token  
**Content Type:** `application/json`  

---

## Table of Contents

1. [Common Response Formats](#1-common-response-formats)
2. [Authentication](#2-authentication--api-v1-auth)
3. [Transactions](#3-transactions--api-v1-transactions)
4. [Categories](#4-categories--api-v1-categories)
5. [Budgets](#5-budgets--api-v1-budgets)
6. [Dashboard](#6-dashboard--api-v1-dashboard)
7. [Reports](#7-reports--api-v1-reports)
8. [Analysis](#8-analysis--api-v1-analysis)
9. [NLP Parsing](#9-nlp-parsing--api-v1-nlp)
10. [Notifications](#10-notifications--api-v1-notifications)
11. [WebSocket](#11-websocket)
12. [Actuator & Swagger](#12-actuator--swagger)
13. [Error Codes Reference](#13-error-codes-reference)

---

## 1. Common Response Formats

### ✅ Success Response (with data)
```json
{
  "success": true,
  "data": { ... }
}
```

### ✅ Success Response (with pagination)
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

### ❌ Error Response
```json
{
  "success": false,
  "error": {
    "code": 400,
    "message": "Human-readable error message",
    "fields": [
      { "field": "email", "message": "Email is required" }
    ]
  }
}
```

### Common HTTP Status Codes
| Code | Meaning |
|------|---------|
| `200` | OK — Success |
| `201` | Created — Resource created |
| `204` | No Content — Deletion success |
| `400` | Bad Request — Validation failure |
| `401` | Unauthorized — Missing/invalid JWT |
| `403` | Forbidden — Insufficient permissions |
| `404` | Not Found — Resource doesn't exist |
| `409` | Conflict — Duplicate resource |
| `429` | Too Many Requests — Rate limited |
| `500` | Internal Server Error |

---

## 2. Authentication — `/api/v1/auth`

All auth endpoints (except those marked **Protected**) are publicly accessible.

---

### 2.1 Register

Creates a new user account. An email verification link is sent to the registered email.

**POST** `/auth/register`

#### Request Body
```json
{
  "email": "user@example.com",
  "password": "Str0ng!Pass",
  "name": "John Doe",
  "currency": "USD"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `email` | string | ✅ | Valid email format |
| `password` | string | ✅ | Min 8 chars, must contain: uppercase, lowercase, digit, special char (`@$!%*?&`) |
| `name` | string | ✅ | Non-blank |
| `currency` | string | ❌ | e.g. `USD`, `EUR`, `GBP` |

#### Response `201 Created`
```json
{
  "success": true,
  "data": {
    "message": "Registration successful. Please verify your email using the link sent to your email."
  }
}
```

---

### 2.2 Verify Email

Verifies a user's email address using the token sent during registration.

**GET** `/auth/verify-email?token={token}`

#### Query Parameters
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `token` | string | ✅ | Email verification token |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Email verified successfully."
  }
}
```

---

### 2.3 Login

Authenticates a user and returns JWT tokens.

**POST** `/auth/login`

#### Request Body
```json
{
  "email": "user@example.com",
  "password": "Str0ng!Pass"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `email` | string | ✅ | Valid email format |
| `password` | string | ✅ | Non-blank |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJl...",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "John Doe",
      "email": "user@example.com",
      "currency": "USD"
    }
  }
}
```

---

### 2.4 Refresh Token

Obtains a new access token using a refresh token.

**POST** `/auth/refresh`

#### Request Body
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `refreshToken` | string | ✅ | Non-blank |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9..."
  }
}
```

---

### 2.5 Logout *(Protected)*

Invalidates the refresh token.

**POST** `/auth/logout`

#### Request Body
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `refreshToken` | string | ✅ | Non-blank |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Logged out successfully"
  }
}
```

---

### 2.6 Forgot Password

Sends a password reset link to the user's email (if the email is registered).

**POST** `/auth/forgot-password`

#### Request Body
```json
{
  "email": "user@example.com"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `email` | string | ✅ | Valid email format |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "If your email is registered, a reset link has been sent."
  }
}
```

> **Note:** This endpoint always returns `200 OK` regardless of whether the email exists (to prevent email enumeration).

---

### 2.7 Reset Password

Resets the password using a token received via email.

**POST** `/auth/reset-password`

#### Request Body
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewStr0ng!Pass"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `token` | string | ✅ | Reset token from email |
| `newPassword` | string | ✅ | Min 8 chars, must contain: uppercase, lowercase, digit, special char |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Password reset successfully."
  }
}
```

---

### 2.8 Get Profile *(Protected)*

Returns the authenticated user's profile.

**GET** `/auth/profile`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "user@example.com",
    "currency": "USD"
  }
}
```

---

### 2.9 Update Profile *(Protected)*

Updates the authenticated user's profile fields.

**PATCH** `/auth/profile`

#### Request Body
```json
{
  "name": "John Updated",
  "currency": "EUR"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | string | ❌ | 2–100 characters |
| `currency` | string | ❌ | Exactly 3 characters (ISO 4217 code) |

> Both fields are optional; only provided fields will be updated.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Updated",
    "email": "user@example.com",
    "currency": "EUR"
  }
}
```

---

### 2.10 Change Password *(Protected)*

Changes the authenticated user's password.

**PATCH** `/auth/change-password`

#### Request Body
```json
{
  "currentPassword": "Str0ng!Pass",
  "newPassword": "NewStr0ng!Pass"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `currentPassword` | string | ✅ | Current password |
| `newPassword` | string | ✅ | Min 8 chars, must contain: uppercase, lowercase, digit, special char |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Password changed successfully."
  }
}
```

---

## 3. Transactions — `/api/v1/transactions`

All transaction endpoints require authentication.

### TransactionType Enum
| Value | Description |
|-------|-------------|
| `INCOME` | Money received |
| `EXPENSE` | Money spent |

---

### 3.1 Create Transaction

**POST** `/transactions`

#### Request Body
```json
{
  "amount": 150.00,
  "type": "EXPENSE",
  "categoryId": "550e8400-e29b-41d4-a716-446655440001",
  "description": "Grocery shopping",
  "date": "2026-06-14"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `amount` | number (BigDecimal) | ✅ | Must be positive |
| `type` | string (enum) | ✅ | `INCOME` or `EXPENSE` |
| `categoryId` | string (UUID) | ✅ | Valid category UUID |
| `description` | string | ❌ | Max 255 characters |
| `date` | string (date) | ✅ | ISO date `yyyy-MM-dd`, must be past or present |

#### Response `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 150.00,
    "type": "EXPENSE",
    "category": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Groceries",
      "isDefault": false
    },
    "description": "Grocery shopping",
    "date": "2026-06-14",
    "createdAt": "2026-06-14T10:30:00Z",
    "updatedAt": "2026-06-14T10:30:00Z"
  }
}
```

---

### 3.2 List Transactions

**GET** `/transactions`

#### Query Parameters
| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `page` | int | ❌ | `1` | Page number (starts at 1) |
| `limit` | int | ❌ | `20` | Items per page (max 100) |
| `from` | date | ❌ | — | Filter: start date (ISO: `yyyy-MM-dd`) |
| `to` | date | ❌ | — | Filter: end date (ISO: `yyyy-MM-dd`) |
| `type` | string | ❌ | — | Filter: `INCOME` or `EXPENSE` |
| `category_id` | UUID | ❌ | — | Filter: category UUID |
| `min_amount` | decimal | ❌ | — | Filter: minimum amount |
| `max_amount` | decimal | ❌ | — | Filter: maximum amount |
| `search` | string | ❌ | — | Full-text search in description |

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440010",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "amount": 150.00,
      "type": "EXPENSE",
      "category": {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Groceries",
        "isDefault": false
      },
      "description": "Grocery shopping",
      "date": "2026-06-14",
      "createdAt": "2026-06-14T10:30:00Z",
      "updatedAt": "2026-06-14T10:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 150,
    "totalPages": 8
  }
}
```

---

### 3.3 Get Transaction by ID

**GET** `/transactions/{id}`

#### Path Parameters
| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Transaction ID |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 150.00,
    "type": "EXPENSE",
    "category": { "id": "uuid", "name": "Groceries", "isDefault": false },
    "description": "Grocery shopping",
    "date": "2026-06-14",
    "createdAt": "2026-06-14T10:30:00Z",
    "updatedAt": "2026-06-14T10:30:00Z"
  }
}
```

#### Error `404 Not Found`
```json
{
  "success": false,
  "error": {
    "code": 404,
    "message": "Transaction not found"
  }
}
```

---

### 3.4 Update Transaction

**PATCH** `/transactions/{id}`

#### Path Parameters
| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Transaction ID |

#### Request Body
```json
{
  "amount": 200.00,
  "type": "EXPENSE",
  "categoryId": "550e8400-e29b-41d4-a716-446655440002",
  "description": "Updated description",
  "date": "2026-06-15"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `amount` | number (BigDecimal) | ❌ | Must be positive |
| `type` | string (enum) | ❌ | `INCOME` or `EXPENSE` |
| `categoryId` | string (UUID) | ❌ | Valid category UUID |
| `description` | string | ❌ | Max 255 characters |
| `date` | string (date) | ❌ | ISO date, must be past or present |

> All fields are optional; only provided fields will be updated.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 200.00,
    "type": "EXPENSE",
    "category": { "id": "uuid", "name": "Utilities", "isDefault": false },
    "description": "Updated description",
    "date": "2026-06-15",
    "createdAt": "2026-06-14T10:30:00Z",
    "updatedAt": "2026-06-15T12:00:00Z"
  }
}
```

---

### 3.5 Delete Transaction

**DELETE** `/transactions/{id}`

#### Path Parameters
| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Transaction ID |

#### Response `204 No Content`
*(No response body)*

#### Error `404 Not Found`
```json
{
  "success": false,
  "error": {
    "code": 404,
    "message": "Transaction not found"
  }
}
```

---

## 4. Categories — `/api/v1/categories`

All category endpoints require authentication.

---

### 4.1 List Categories

**GET** `/categories`

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Groceries",
      "isDefault": false
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "name": "Uncategorized",
      "isDefault": true
    }
  ]
}
```

> Includes both user-created categories and the default "Uncategorized" fallback category.

---

### 4.2 Create Category

**POST** `/categories`

#### Request Body
```json
{
  "name": "Transportation"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | string | ✅ | 2–100 characters |

#### Response `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "name": "Transportation",
    "isDefault": false
  }
}
```

#### Error `409 Conflict` (duplicate name)
```json
{
  "success": false,
  "error": {
    "code": 409,
    "message": "Category with this name already exists"
  }
}
```

---

### 4.3 Update Category

**PATCH** `/categories/{id}`

#### Path Parameters
| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Category ID |

#### Request Body
```json
{
  "name": "Public Transport"
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "name": "Public Transport",
    "isDefault": false
  }
}
```

---

### 4.4 Delete Category

**DELETE** `/categories/{id}`

#### Path Parameters
| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Category ID |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Category deleted. Transactions reassigned to Uncategorized."
  }
}
```

> Transactions belonging to the deleted category are automatically reassigned to the default "Uncategorized" category.

#### Error `404 Not Found`
```json
{
  "success": false,
  "error": {
    "code": 404,
    "message": "Category not found"
  }
}
```

---

## 5. Budgets — `/api/v1/budgets`

All budget endpoints require authentication.

---

### 5.1 List Budgets

**GET** `/budgets`

#### Query Parameters
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `month` | short (1–12) | ❌ | Filter by month |
| `year` | short (2000–2100) | ❌ | Filter by year |

> If no month/year is provided, returns all budgets across all periods.

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440020",
      "category": {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Groceries",
        "isDefault": false
      },
      "limitAmount": 500.00,
      "spent": 320.50,
      "remaining": 179.50,
      "percentage": 64.1,
      "status": "ok",
      "month": 6,
      "year": 2026
    }
  ]
}
```

**`status` field values:**
| Status | Meaning |
|--------|---------|
| `ok` | Under budget |
| `warning` | 80%+ of budget used |
| `exceeded` | Over budget |

---

### 5.2 Create Budget

**POST** `/budgets`

#### Request Body
```json
{
  "categoryId": "550e8400-e29b-41d4-a716-446655440001",
  "limitAmount": 500.00,
  "month": 6,
  "year": 2026
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `categoryId` | UUID | ✅ | Valid category UUID |
| `limitAmount` | decimal | ✅ | Must be positive |
| `month` | short | ✅ | 1–12 |
| `year` | short | ✅ | 2000–2100 |

#### Response `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440020",
    "category": { "id": "uuid", "name": "Groceries", "isDefault": false },
    "limitAmount": 500.00,
    "spent": 0.00,
    "remaining": 500.00,
    "percentage": 0.00,
    "status": "ok",
    "month": 6,
    "year": 2026
  }
}
```

> Newly created budgets start with `spent = 0`, `remaining = limitAmount`, `percentage = 0`, `status = "ok"`.

---

### 5.3 Update Budget

**PATCH** `/budgets/{id}`

#### Path Parameters
| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Budget ID |

#### Request Body
```json
{
  "limitAmount": 600.00
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `limitAmount` | decimal | ✅ | Must be positive |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440020",
    "category": { "id": "uuid", "name": "Groceries", "isDefault": false },
    "limitAmount": 600.00,
    "spent": null,
    "remaining": null,
    "percentage": null,
    "status": null,
    "month": 6,
    "year": 2026
  }
}
```

> After update, spending/remaining/percentage/status are returned as `null` (recalculated on next fetch via GET).

---

### 5.4 Delete Budget

**DELETE** `/budgets/{id}`

#### Path Parameters
| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Budget ID |

#### Response `204 No Content`
*(No response body)*

---

## 6. Dashboard — `/api/v1/dashboard`

Requires authentication.

### 6.1 Get Dashboard Summary

**GET** `/dashboard`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "totalIncome": 4500.00,
    "totalExpenses": 3200.50,
    "balance": 1299.50,
    "monthlyIncome": 4500.00,
    "monthlyExpenses": 3200.50,
    "recentTransactions": [
      {
        "id": "uuid",
        "amount": 150.00,
        "type": "EXPENSE",
        "category": { "id": "uuid", "name": "Groceries", "isDefault": false },
        "description": "Grocery shopping",
        "date": "2026-06-14"
      }
    ],
    "budgetOverview": [
      {
        "id": "uuid",
        "category": { "id": "uuid", "name": "Groceries", "isDefault": false },
        "limitAmount": 500.00,
        "spent": 320.50,
        "remaining": 179.50,
        "percentage": 64.1,
        "status": "ok",
        "month": 6,
        "year": 2026
      }
    ]
  }
}
```

> The dashboard returns aggregated data including totals, recent transactions, and current month's budget overview.

---

## 7. Reports — `/api/v1/reports`

All report endpoints require authentication.

---

### 7.1 Category Distribution

**GET** `/reports/category-distribution`  
*(Alias: `/reports/distribution`)*

#### Query Parameters
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `from` | date | ❌ | Start date (ISO: `yyyy-MM-dd`) |
| `to` | date | ❌ | End date (ISO: `yyyy-MM-dd`) |

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "category": "Groceries",
      "total": 320.50,
      "percentage": 25.4,
      "count": 12,
      "type": "EXPENSE"
    },
    {
      "category": "Salary",
      "total": 5000.00,
      "percentage": 100.0,
      "count": 1,
      "type": "INCOME"
    }
  ]
}
```

---

### 7.2 Monthly Comparison

**GET** `/reports/monthly-comparison`  
*(Alias: `/reports/monthly`)*

#### Query Parameters
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `from` | date | ❌ | Start date |
| `to` | date | ❌ | End date |

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "month": "2026-01",
      "income": 4500.00,
      "expense": 2800.00,
      "net": 1700.00
    },
    {
      "month": "2026-02",
      "income": 4500.00,
      "expense": 3100.00,
      "net": 1400.00
    }
  ]
}
```

---

### 7.3 Daily Spending

**GET** `/reports/daily-spending`  
*(Alias: `/reports/daily`)*

#### Query Parameters
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `from` | date | ❌ | Start date |
| `to` | date | ❌ | End date |

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "date": "2026-06-01",
      "total": 120.50,
      "count": 3
    },
    {
      "date": "2026-06-02",
      "total": 85.00,
      "count": 1
    }
  ]
}
```

---

### 7.4 Summary Statistics

**GET** `/reports/summary`

#### Query Parameters
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `from` | date | ✅ | Start date (ISO: `yyyy-MM-dd`) |
| `to` | date | ✅ | End date (ISO: `yyyy-MM-dd`) |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "totalIncome": 4500.00,
    "totalExpenses": 3200.50,
    "netSavings": 1299.50,
    "transactionCount": 45,
    "averageTransaction": 71.12,
    "largestExpense": 500.00,
    "largestIncome": 4500.00,
    "period": {
      "from": "2026-06-01",
      "to": "2026-06-30"
    }
  }
}
```

---

### 7.5 Export to CSV

**GET** `/reports/export`

Downloads transactions as a CSV file for the specified date range.

#### Query Parameters
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `from` | date | ✅ | Start date (ISO: `yyyy-MM-dd`) |
| `to` | date | ✅ | End date (ISO: `yyyy-MM-dd`) |

#### Response `200 OK`
```
Content-Type: text/csv
Content-Disposition: attachment; filename="transactions_2026-06-01_2026-06-30.csv"

id,date,type,category,amount,description
uuid,2026-06-14,EXPENSE,Groceries,150.00,"Grocery shopping"
```

> The response has `Content-Type: text/csv` and `Content-Disposition: attachment` headers for file download.

---

## 8. Analysis — `/api/v1/analysis`

All analysis endpoints require authentication. These endpoints use AI (Spring AI with OpenAI/NVIDIA NIM) + statistical methods to provide financial insights.

---

### 8.1 Get Insights

**GET** `/analysis/insights`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "sufficient": true,
    "cachedAt": "2026-06-14T10:30:00Z",
    "patterns": [
      "You spend 40% more on dining out on weekends",
      "Your grocery spending has increased 15% this month"
    ],
    "anomalies": [
      {
        "transactionId": "uuid",
        "date": "2026-06-10",
        "amount": 500.00,
        "category": "Entertainment",
        "average": 120.00,
        "deviation": "+316%",
        "reason": "This transaction is 3.2 standard deviations above the mean for this category"
      }
    ],
    "recommendations": [
      "Consider setting a weekly dining budget of $100",
      "You could save $200/month by reducing subscription services"
    ],
    "projectedMonthlyExpense": 3400.00,
    "message": null
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `sufficient` | boolean | Whether there's enough data for meaningful analysis |
| `cachedAt` | ISO datetime | When the AI analysis was cached |
| `patterns` | string[] | AI-detected spending patterns |
| `anomalies` | array | Unusual transactions (see below) |
| `recommendations` | string[] | AI-generated savings recommendations |
| `projectedMonthlyExpense` | decimal | Projected end-of-month expense |
| `message` | string/null | Informational message if data is insufficient |

---

### 8.2 Get Anomalies

**GET** `/analysis/anomalies`

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "transactionId": "550e8400-e29b-41d4-a716-446655440010",
      "date": "2026-06-10",
      "amount": 500.00,
      "category": "Entertainment",
      "average": 120.00,
      "deviation": "+316%",
      "reason": "This transaction is 3.2 standard deviations above the mean for this category"
    }
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `transactionId` | UUID | The anomalous transaction |
| `date` | date | Transaction date |
| `amount` | decimal | Transaction amount |
| `category` | string | Category name |
| `average` | decimal | Average transaction amount for this category |
| `deviation` | string | Deviation from average (percentage) |
| `reason` | string | Explanation of why it's anomalous |

---

### 8.3 Get Projection

**GET** `/analysis/projection`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "spent": 2500.00,
    "projected": 3400.00,
    "daysElapsed": 14,
    "daysInMonth": 30,
    "currency": "USD"
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `spent` | decimal | Total spent so far this month |
| `projected` | decimal | Projected end-of-month spend |
| `daysElapsed` | int | Days elapsed in the current month |
| `daysInMonth` | int | Total days in the current month |
| `currency` | string | User's currency setting |

---

## 9. NLP Parsing — `/api/v1/nlp`

Requires authentication.

### 9.1 Parse Natural Language Input

Accepts a natural language description of a transaction and attempts to parse it into structured transaction data using AI.

**POST** `/nlp/parse`

#### Request Body
```json
{
  "text": "I spent $50 on gas for my car yesterday"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `text` | string | ✅ | Non-blank |

#### Response `200 OK` (successfully parsed)
```json
{
  "success": true,
  "data": {
    "parsed": true,
    "draft": {
      "amount": 50.00,
      "type": "expense",
      "categoryId": null,
      "categoryName": "Transportation",
      "description": "Gas for car",
      "date": "2026-06-13"
    }
  }
}
```

#### Response `200 OK` (failed to parse)
```json
{
  "success": true,
  "data": {
    "parsed": false,
    "message": "Could not extract transaction details."
  }
}
```

**`DraftTransactionDTO` fields:**
| Field | Type | Description |
|-------|------|-------------|
| `amount` | decimal | Extracted amount (null if not found) |
| `type` | string | `"expense"`, `"income"`, or `"transfer"` |
| `categoryId` | string/`null` | Matched category UUID (if exists in user's categories) |
| `categoryName` | string | Extracted or inferred category name |
| `description` | string | Cleaned description |
| `date` | date | Parsed/relative date (ISO `yyyy-MM-dd`) |

> The NLP endpoint uses Spring AI with an LLM provider (OpenAI or NVIDIA NIM, configured via `application.yaml`) and falls back to Apache OpenNLP for basic parsing.

---

## 10. Notifications — `/api/v1/notifications`

All notification endpoints require authentication. Notifications are generated automatically for budget alerts, anomaly detection, etc.

---

### 10.1 List Notifications

**GET** `/notifications`

#### Query Parameters
| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `unread_only` | boolean | ❌ | `false` | Filter to only unread notifications |
| `page` | int | ❌ | `1` | Page number |
| `limit` | int | ❌ | `20` | Items per page |

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440030",
      "type": "BUDGET_ALERT",
      "title": "Budget Warning",
      "body": "You have used 85% of your Groceries budget for June",
      "isRead": false,
      "createdAt": "2026-06-14T10:30:00"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

**Notification types:**
| Type | Description |
|------|-------------|
| `BUDGET_ALERT` | Budget threshold reached |
| `ANOMALY_DETECTED` | Unusual transaction detected |
| `INSIGHT_READY` | New AI insight available |

---

### 10.2 Mark Notification as Read

**PATCH** `/notifications/{id}/read`

#### Path Parameters
| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Notification ID |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Notification marked as read."
  }
}
```

---

### 10.3 Mark All Notifications as Read

**PATCH** `/notifications/read-all`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "All notifications marked as read."
  }
}
```

---

## 11. WebSocket

The application supports real-time notifications via STOMP over WebSocket (with SockJS fallback).

### Connection Details
| Property | Value |
|----------|-------|
| **Endpoint** | `/ws` |
| **Protocol** | STOMP over WebSocket |
| **Fallback** | SockJS |
| **Broker Prefix** | `/topic` |
| **Application Prefix** | `/app` |

### Connecting
```javascript
// Using STOMP.js
const stompClient = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: {} // No auth required for WebSocket handshake
});

stompClient.onConnect = () => {
  stompClient.subscribe('/topic/notifications', message => {
    const notification = JSON.parse(message.body);
    console.log('New notification:', notification);
  });
};

stompClient.activate();
```

### Subscribable Topics
| Topic | Payload | Description |
|-------|---------|-------------|
| `/topic/notifications` | `NotificationResponse` | Real-time notifications pushed to user |
| `/topic/budget-alerts` | `BudgetResponse` | Budget threshold alerts |

### SockJS Fallback
```javascript
const stompClient = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  // ...
});
```

---

## 12. Actuator & Swagger

### Health Check
**GET** `/actuator/health`

```json
{
  "status": "UP"
}
```

### Swagger UI
**URL:** `/api/v1/swagger/swagger-ui.html` (or `/api/v1/swagger-ui/**`)

### OpenAPI JSON Spec
**URL:** `/api/v1/swagger/api-docs` (or `/api/v1/v3/api-docs/**`)

> Both Swagger and Actuator endpoints are **publicly accessible** (no authentication required).

---

## 13. Error Codes Reference

### HTTP Status Codes by Endpoint

| Endpoint | 400 | 401 | 403 | 404 | 409 | 429 |
|----------|-----|-----|-----|-----|-----|-----|
| Auth Register | ✅ | — | — | — | ✅ | ✅ |
| Auth Login | ✅ | ✅ | — | — | — | ✅ |
| Auth Refresh | ✅ | ✅ | — | — | — | ✅ |
| Auth Profile | — | ✅ | — | — | — | ✅ |
| Auth Change Password | ✅ | ✅ | ✅ | — | — | ✅ |
| Transactions | ✅ | ✅ | — | ✅ | — | ✅ |
| Categories | ✅ | ✅ | — | ✅ | ✅ | ✅ |
| Budgets | ✅ | ✅ | — | ✅ | — | ✅ |
| Reports | ✅ | ✅ | — | — | — | ✅ |
| Dashboard | — | ✅ | — | — | — | ✅ |
| Analysis | — | ✅ | — | — | — | ✅ |
| NLP Parse | ✅ | ✅ | — | — | — | ✅ |
| Notifications | — | ✅ | — | ✅ | — | ✅ |

### Common Error Responses

#### Validation Error `400`
```json
{
  "success": false,
  "error": {
    "code": 400,
    "message": "Validation failed",
    "fields": [
      { "field": "email", "message": "Email is required" },
      { "field": "password", "message": "Password must be at least 8 characters" }
    ]
  }
}
```

#### Unauthorized `401`
```json
{
  "success": false,
  "error": {
    "code": 401,
    "message": "Full authentication is required to access this resource"
  }
}
```

#### Forbidden `403`
```json
{
  "success": false,
  "error": {
    "code": 403,
    "message": "You do not have permission to access this resource"
  }
}
```

#### Not Found `404`
```json
{
  "success": false,
  "error": {
    "code": 404,
    "message": "Resource not found"
  }
}
```

#### Conflict `409`
```json
{
  "success": false,
  "error": {
    "code": 409,
    "message": "Category with this name already exists"
  }
}
```

#### Rate Limited `429`
```json
{
  "success": false,
  "error": {
    "code": 429,
    "message": "Too many requests — please try again later"
  }
}
```

---

## Authentication Summary

### How to Authenticate

1. **Register** (`POST /auth/register`) or **Login** (`POST /auth/login`)
2. Receive `accessToken` and `refreshToken`
3. Include the `accessToken` in all protected requests as a Bearer token:

```
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

4. When the access token expires, use `POST /auth/refresh` with your refresh token to get a new one
5. Use `POST /auth/logout` to invalidate the refresh token

### Public Endpoints (No Auth Needed)
```
POST   /api/v1/auth/register
GET    /api/v1/auth/verify-email
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
GET    /api/v1/swagger/**
GET    /api/v1/swagger-ui/**
GET    /api/v1/v3/api-docs/**
GET    /actuator/**
WS     /ws/**
```

### Protected Endpoints (Auth Required)
```
POST   /api/v1/auth/logout
GET    /api/v1/auth/profile
PATCH  /api/v1/auth/profile
PATCH  /api/v1/auth/change-password
GET    /api/v1/categories
POST   /api/v1/categories
PATCH  /api/v1/categories/{id}
DELETE /api/v1/categories/{id}
GET    /api/v1/budgets
POST   /api/v1/budgets
PATCH  /api/v1/budgets/{id}
DELETE /api/v1/budgets/{id}
GET    /api/v1/transactions
POST   /api/v1/transactions
GET    /api/v1/transactions/{id}
PATCH  /api/v1/transactions/{id}
DELETE /api/v1/transactions/{id}
GET    /api/v1/dashboard
GET    /api/v1/reports/**
GET    /api/v1/analysis/**
POST   /api/v1/nlp/parse
GET    /api/v1/notifications
PATCH  /api/v1/notifications/{id}/read
PATCH  /api/v1/notifications/read-all
```

---

*Documentation generated from source code. For the most up-to-date spec, run the application and visit `/api/v1/swagger/swagger-ui.html`.*
