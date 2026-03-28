# Low Level Design

## FinTrack AI - Intelligent Personal Finance Management System

**Version:** 1.0  
**Date:** March 28, 2026  
**Status:** Draft  
**Reference:** SRS v1.0, HLD v1.0

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Module Breakdown](#2-module-breakdown)
3. [Authentication Module](#3-authentication-module)
4. [Transaction Module](#4-transaction-module)
5. [Category Module](#5-category-module)
6. [Budget Module](#6-budget-module)
7. [NLP Parser Module](#7-nlp-parser-module)
8. [AI Analysis Module](#8-ai-analysis-module)
9. [Reporting Module](#9-reporting-module)
10. [Notification Module](#10-notification-module)
11. [Frontend Component Design](#11-frontend-component-design)
12. [Error Handling Strategy](#12-error-handling-strategy)
13. [Logging Strategy](#13-logging-strategy)
14. [Revision History](#14-revision-history)

---

## 1. Introduction

### 1.1 Purpose

This Low Level Design (LLD) document provides implementation-level specifications for all backend modules and frontend components of FinTrack AI. It defines function signatures, class responsibilities, data validation rules, algorithm descriptions, and inter-module communication contracts.

### 1.2 Scope

This document covers all backend service modules, data access repositories, middleware components, and frontend component trees. It serves as the direct reference for developers writing code.

### 1.3 Audience

Backend developers, frontend developers, and technical reviewers involved in the implementation of FinTrack AI.

---

## 2. Module Breakdown

The backend is organized into the following Spring Boot package structure:

```
com.fintrack.ai/
  config/
    SecurityConfig.java      # Spring Security & JWT Filter
    RedisConfig.java         # RedisTemplate & Cache manager
    WebSocketConfig.java     # WebSocket & STOMP setup
    AppConfig.java           # General beans (Ollama client, etc.)
  controller/                # REST Controllers (API Layer)
    AuthController.java
    TransactionController.java
    CategoryController.java
    BudgetController.java
    NLPController.java
    AnalysisController.java
    ReportController.java
    NotificationController.java
  service/                   # Business Logic Layer
    AuthService.java
    TransactionService.java
    CategoryService.java
    BudgetService.java
    NLPService.java
    AnalysisService.java
    ReportService.java
    NotificationService.java
    EmailService.java
    WebSocketService.java
  repository/                # Spring Data JPA Repositories
    UserRepository.java
    TransactionRepository.java
    CategoryRepository.java
    BudgetRepository.java
    RefreshTokenRepository.java
  entity/                    # JPA Entities (Database Models)
    User.java
    Transaction.java
    Category.java
    Budget.java
    RefreshToken.java
    Notification.java
  dto/                       # Data Transfer Objects
    request/                 # Request payloads (with Validation)
    response/                # API response envelopes
  exception/                 # Custom Exception handling
    GlobalExceptionHandler.java
    AppException.java
  security/                  # JWT & Auth providers
    JwtProvider.java
    UserDetailsServiceImpl.java
  util/                      # Helper classes
    DateUtils.java
    CurrencyUtils.java
```

---

## 3. Authentication Module

### 3.1 DTO Validation

Validation is handled via Jakarta Bean Validation (Hibernate Validator) annotations on DTOs.

```java
public class RegisterRequest {
    @Email @NotBlank String email;
    @Size(min = 8) @Pattern(regexp = "...") String password;
    @NotBlank String name;
}

public class LoginRequest {
    @Email @NotBlank String email;
    @NotBlank String password;
}
```

### 3.2 AuthService.java

**Method: registerUser(RegisterRequest request)**

```
1. Check if email exists via userRepository.existsByEmail(request.getEmail())
2. If exists, throw ConflictException("Email already registered")
3. Encode password: passwordEncoder.encode(request.getPassword())
4. Generate verification token: UUID.randomUUID().toString()
5. Create User entity with encoded password and verification token
6. Save user via userRepository.save(user)
7. Call emailService.sendVerificationEmail(user.getEmail(), token)
8. Return SuccessResponse
```

**Method: loginUser(LoginRequest request)**

```
1. Authenticate via authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))
2. If fails, Spring Security throws 401
3. If user not verified, throw ForbiddenException
4. Generate JWT accessToken: jwtProvider.generateAccessToken(userDetails)
5. Generate JWT refreshToken: jwtProvider.generateRefreshToken(userDetails)
6. Save refreshToken hash in Redis or DB
7. Return AuthResponse { accessToken, refreshToken, userDetails }
```

### 3.3 Security Configuration

**SecurityConfig.java**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Configure SecurityFilterChain
    // .authorizeHttpRequests() -> permitAll for /api/auth/**
    // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
}
```

### 3.4 auth.repository.js

| Function | Query |
|----------|-------|
| findByEmail(email) | SELECT * FROM users WHERE email = $1 |
| createUser(data) | INSERT INTO users (...) VALUES (...) RETURNING * |
| findByVerificationToken(token) | SELECT * FROM users WHERE verification_token = $1 |
| markEmailVerified(id) | UPDATE users SET email_verified = true, verification_token = null WHERE id = $1 |
| saveRefreshToken(userId, hash) | INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (...) |
| findRefreshToken(hash) | SELECT * FROM refresh_tokens WHERE token_hash = $1 AND expires_at > NOW() |
| deleteRefreshToken(hash) | DELETE FROM refresh_tokens WHERE token_hash = $1 |
| updatePassword(id, hash) | UPDATE users SET password_hash = $1 WHERE id = $2 |

---

## 4. Transaction Module

### 4.1 transaction.validators.js

```
createTransactionSchema:
  - amount: number, positive, max 2 decimal places, required
  - type: enum ["income", "expense"], required
  - category_id: UUID string, required
  - description: string, max 255 chars, optional
  - date: ISO 8601 date string, required, not future date

updateTransactionSchema:
  - amount: number, positive, optional
  - type: enum ["income", "expense"], optional
  - category_id: UUID string, optional
  - description: string, max 255 chars, optional
  - date: ISO 8601 date string, optional

transactionQuerySchema:
  - page: integer, min 1, default 1
  - limit: integer, min 1, max 100, default 20
  - from: ISO date, optional
  - to: ISO date, optional
  - type: enum ["income", "expense"], optional
  - category_id: UUID, optional
  - search: string, max 100 chars, optional
  - min_amount: number, optional
  - max_amount: number, optional
```

### 4.2 transaction.service.js

**Function: createTransaction(userId, data)**

```
1. Validate that category_id belongs to userId or is a system default
   via category.repository.findByIdAndUser(categoryId, userId)
2. If not found, throw NotFoundError("Category not found")
3. Call transaction.repository.create({ userId, ...data })
4. Call budget.service.checkBudgetAfterTransaction(userId, data.category_id, data.date)
5. Return created transaction object
```

**Function: getTransactions(userId, query)**

```
1. Destructure query: { page, limit, from, to, type, category_id, search, min_amount, max_amount }
2. Build filter object:
   - WHERE user_id = userId
   - AND date BETWEEN from AND to (if provided)
   - AND type = type (if provided)
   - AND category_id = category_id (if provided)
   - AND amount BETWEEN min_amount AND max_amount (if provided)
   - AND description ILIKE '%search%' (if provided)
3. Calculate offset = (page - 1) * limit
4. Call transaction.repository.findMany(filter, limit, offset)
5. Call transaction.repository.count(filter)
6. Return { data, pagination: { page, limit, total, totalPages } }
```

**Function: updateTransaction(userId, transactionId, data)**

```
1. Call transaction.repository.findByIdAndUser(transactionId, userId)
2. If not found, throw NotFoundError
3. If category_id in data, validate ownership as in createTransaction
4. Call transaction.repository.update(transactionId, data)
5. Call budget.service.checkBudgetAfterTransaction(userId, effectiveCategoryId, effectiveDate)
6. Return updated transaction
```

**Function: deleteTransaction(userId, transactionId)**

```
1. Call transaction.repository.findByIdAndUser(transactionId, userId)
2. If not found, throw NotFoundError
3. Call transaction.repository.delete(transactionId)
4. Return { message: "Transaction deleted." }
```

**Function: getDashboardSummary(userId)**

```
1. Compute start and end of current calendar month
2. Call transaction.repository.sumByType(userId, startOfMonth, endOfMonth)
   Returns: { totalIncome, totalExpenses }
3. netBalance = totalIncome - totalExpenses
4. Call transaction.repository.findRecent(userId, limit=5)
5. Return { totalIncome, totalExpenses, netBalance, recentTransactions }
```

### 4.3 transaction.repository.js

Key query descriptions:

**create(data):** INSERT into transactions table, return created row.

**findMany(filter, limit, offset):** Dynamic WHERE clause built from filter object, ORDER BY date DESC, LIMIT and OFFSET applied.

**count(filter):** Same WHERE clause as findMany without LIMIT/OFFSET, returns integer count.

**findByIdAndUser(id, userId):** SELECT WHERE id = $1 AND user_id = $2.

**update(id, data):** Dynamic SET clause, UPDATE WHERE id = $1, return updated row.

**delete(id):** DELETE WHERE id = $1.

**sumByType(userId, from, to):** SELECT type, SUM(amount) GROUP BY type WHERE user_id AND date BETWEEN.

**findRecent(userId, limit):** SELECT WHERE user_id ORDER BY date DESC LIMIT $2.

---

## 5. Category Module

### 5.1 category.service.js

**Function: getCategories(userId)**

```
1. Call category.repository.findAllForUser(userId)
   Returns: system defaults (user_id IS NULL) UNION user's custom categories
2. Return category list
```

**Function: createCategory(userId, name)**

```
1. Call category.repository.findByNameAndUser(name, userId)
2. If exists, throw ConflictError("Category name already exists")
3. Call category.repository.create({ userId, name, isDefault: false })
4. Return created category
```

**Function: updateCategory(userId, categoryId, name)**

```
1. Call category.repository.findByIdAndUser(categoryId, userId)
2. If not found or isDefault is true, throw ForbiddenError("Cannot modify system category")
3. Call category.repository.update(categoryId, { name })
4. Return updated category
```

**Function: deleteCategory(userId, categoryId)**

```
1. Call category.repository.findByIdAndUser(categoryId, userId)
2. If not found, throw NotFoundError
3. If isDefault is true, throw ForbiddenError
4. Find system "Uncategorized" category id
5. Call transaction.repository.reassignCategory(userId, categoryId, uncategorizedId)
6. Call category.repository.delete(categoryId)
7. Return { message: "Category deleted. Transactions reassigned to Uncategorized." }
```

---

## 6. Budget Module

### 6.1 budget.validators.js

```
createBudgetSchema:
  - category_id: UUID, required
  - limit_amount: number, positive, required
  - month: integer, 1-12, required
  - year: integer, min 2000, max 2100, required

updateBudgetSchema:
  - limit_amount: number, positive, optional
```

### 6.2 budget.service.js

**Function: createBudget(userId, data)**

```
1. Validate category ownership
2. Check if budget already exists for (userId, category_id, month, year)
   If exists, throw ConflictError
3. Call budget.repository.create({ userId, ...data })
4. Return created budget
```

**Function: getBudgets(userId)**

```
1. Get current month and year
2. Call budget.repository.findByUserAndPeriod(userId, month, year)
3. For each budget, call transaction.repository.sumByCategoryAndPeriod(userId, category_id, month, year)
4. Compute: { spent, limit, percentage, status: "ok" | "warning" | "exceeded" }
5. Return enriched budget list
```

**Function: checkBudgetAfterTransaction(userId, categoryId, date)**

```
1. Parse month and year from date
2. Call budget.repository.findByUserCategoryAndPeriod(userId, categoryId, month, year)
3. If no budget found, return (no action)
4. Call transaction.repository.sumByCategoryAndPeriod(userId, categoryId, month, year)
5. Calculate percentage = (totalSpent / budget.limitAmount) * 100
6. If percentage >= 100:
   notification.service.sendBudgetAlert(userId, categoryName, "exceeded", totalSpent, limitAmount)
7. Else if percentage >= 80:
   notification.service.sendBudgetAlert(userId, categoryName, "warning", totalSpent, limitAmount)
```

---

## 7. NLP Parser Module

### 7.1 NLPService.java

**Method: parseNaturalLanguageInput(Long userId, String text)**

```
1. Fetch user's categories from CategoryService
2. Construct prompt for local Ollama API:
   "Extract financial transaction data from the text: '{text}'. 
    Categories available: {categoryList}. 
    Output JSON: {amount, categoryName, description}."
3. Send POST request to http://fintrack-ollama:11434/api/generate
4. Parse JSON response using Jackson
5. Validate amount and category mapping
6. Return DraftTransactionDTO
```

---

## 8. AI Analysis Module

### 8.1 AnalysisService.java

**Method: getInsights(Long userId)**

```
1. Check Redis for cached insights: "insights:" + userId
2. If cache hit, return results
3. Aggregate last 90 days of transactions using JPA @Query
4. Construct summary prompt for Ollama
5. Call Ollama API for patterns and recommendations
6. Store result in Redis with 6h TTL
7. Return InsightResponse
```

---

## 9. Reporting Module

### 9.1 report.service.js

**Function: getCategoryDistribution(userId, from, to)**

```
1. Call report.repository.sumByCategory(userId, from, to, type='expense')
2. Format: [{ category_name, total, percentage }]
3. Return formatted data
```

**Function: getMonthlyComparison(userId)**

```
1. Compute last 6 complete months (start and end dates)
2. For each month, call transaction.repository.sumByType(userId, start, end)
3. Return: [{ month: "Jan 2026", income, expenses }] (array of 6)
```

**Function: getDailySpendingLine(userId, year, month)**

```
1. Compute start and end of specified month
2. Call report.repository.dailySumByDate(userId, start, end, type='expense')
3. Fill in 0 for days with no transactions
4. Compute cumulative running total
5. Return: [{ date: "2026-03-01", daily, cumulative }]
```

**Function: exportTransactionsCSV(userId, from, to)**

```
1. Fetch all transactions with category names within date range (no pagination)
2. Build CSV string:
   Header: Date, Type, Category, Description, Amount
   Rows: one per transaction
3. Return CSV string with Content-Type: text/csv
```

**Function: getSummaryStatistics(userId, from, to)**

```
1. totalIncome: SUM of income transactions in range
2. totalExpenses: SUM of expense transactions in range
3. largestExpense: MAX single expense transaction in range
4. transactionCount: COUNT of transactions in range
5. averageDailySpend: totalExpenses / number of days in range
6. Return { totalIncome, totalExpenses, netBalance, largestExpense, transactionCount, averageDailySpend }
```

---

## 10. Notification Module

### 10.1 WebSocketService.java

**Method: pushNotification(Long userId, NotificationPayload payload)**

```
1. Use SimpMessagingTemplate to send message
2. Destination: "/topic/notifications/" + userId
3. Client (React/JavaFX/Android) receives message on subscribed topic
```

---

## 11. Frontend Component Design

### 11.1 Component Tree

```
App
  AuthProvider (manages JWT, user context)
  Router
    PublicRoutes
      LoginPage
      RegisterPage
      VerifyEmailPage
      ForgotPasswordPage
      ResetPasswordPage
    ProtectedRoutes (require valid JWT)
      Layout
        Navbar
        NotificationListener (WebSocket connection)
        Sidebar
        Outlet (renders child route)
      DashboardPage
        SummaryCards (income, expenses, balance)
        RecentTransactions
        SpendingMiniChart
      TransactionsPage
        TransactionFilters
        TransactionTable
          TransactionRow
        PaginationControls
        AddTransactionModal
          ManualTransactionForm
          NLPInputForm
      CategoriesPage
        CategoryList
          CategoryItem
        AddCategoryModal
        RenameCategoryModal
      BudgetsPage
        BudgetList
          BudgetCard (progress bar, amounts)
        AddBudgetModal
      ReportsPage
        DateRangePicker
        CategoryPieChart (Recharts PieChart)
        MonthlyBarChart (Recharts BarChart)
        DailyLineChart (Recharts LineChart)
        SummaryStatsPanel
        ExportCSVButton
      SettingsPage
        ProfileForm
        ChangePasswordForm
        NotificationPreferences
```

### 11.2 State Management (Redux Slices)

| Slice | State Fields |
|-------|-------------|
| authSlice | user, accessToken, isAuthenticated, loading |
| transactionSlice | transactions, pagination, filters, loading, error |
| categorySlice | categories, loading |
| budgetSlice | budgets, loading |
| reportSlice | distribution, monthly, daily, stats, loading |
| analysisSlice | insights, anomalies, projection, loading |
| notificationSlice | alerts (queue of unread alerts) |

### 11.3 API Communication

All API calls use a shared Axios instance configured with:

- baseURL from environment variable
- Request interceptor: attaches Authorization: Bearer {accessToken}
- Response interceptor: on 401, attempts token refresh; on second 401, dispatches logout action

---

## 12. Error Handling Strategy

### 12.1 Custom Error Classes

```
AppError (base)
  - statusCode: number
  - message: string
  - isOperational: boolean (true = expected errors, false = bugs)

ConflictError extends AppError (409)
NotFoundError extends AppError (404)
UnauthorizedError extends AppError (401)
ForbiddenError extends AppError (403)
BadRequestError extends AppError (400)
ValidationError extends AppError (422)
```

### 12.2 Global Error Handler Middleware

```
errorHandler(err, req, res, next):
  1. If err is AppError and isOperational:
     Return JSON: { success: false, error: { code: statusCode, message: err.message } }
  2. Else (unexpected error):
     Log full stack trace
     Return JSON: { success: false, error: { code: 500, message: "Internal server error" } }
```

### 12.3 Frontend Error Handling

- API errors are caught in Redux async thunks
- Validation errors (422) are mapped to individual form field errors
- Auth errors (401) trigger token refresh or redirect to login
- Generic errors display a toast notification with a user-friendly message

---

## 13. Logging Strategy

### 13.1 Backend Logging (SLF4J + Logback)

Standard logging implementation for Spring Boot.

Log levels: ERROR, WARN, INFO, DEBUG

**Request Logging:** Handled via a Custom Request Interceptor or Filter.
**Error Logging:** Handled in `GlobalExceptionHandler`.
**AI Logging:** Log prompt length and Ollama response latency.

### 13.2 Log Retention

Logs are retained for 90 days as defined in the SRS. In production, logs are streamed to the hosting provider's log management service.

---

## 14. Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | March 28, 2026 | Project Team | Initial draft |