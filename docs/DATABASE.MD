# Database Design

## FinTrack AI - Intelligent Personal Finance Management System

**Version:** 1.0  
**Date:** March 28, 2026  
**Status:** Draft  
**Reference:** SRS v1.0, HLD v1.0, LLD v1.0

---

## Table of Contents

- [Database Design](#database-design)
  - [FinTrack AI - Intelligent Personal Finance Management System](#fintrack-ai---intelligent-personal-finance-management-system)
  - [Table of Contents](#table-of-contents)
  - [1. Introduction](#1-introduction)
  - [2. Database Technology](#2-database-technology)
  - [3. Entity Relationship Overview](#3-entity-relationship-overview)
  - [4. Table Definitions](#4-table-definitions)
    - [4.1 users](#41-users)
    - [4.2 refresh\_tokens](#42-refresh_tokens)
    - [4.3 password\_reset\_tokens](#43-password_reset_tokens)
    - [4.4 categories](#44-categories)
    - [4.5 transactions](#45-transactions)
    - [4.6 budgets](#46-budgets)
    - [4.7 notifications](#47-notifications)
  - [5. Relationships and Foreign Keys](#5-relationships-and-foreign-keys)
  - [6. Indexes](#6-indexes)
    - [6.1 Primary Key Indexes (Automatic)](#61-primary-key-indexes-automatic)
    - [6.2 Application Indexes](#62-application-indexes)
    - [6.3 Index Strategy Notes](#63-index-strategy-notes)
  - [7. Constraints and Business Rules](#7-constraints-and-business-rules)
  - [8. Seed Data](#8-seed-data)
  - [9. Migration Strategy](#9-migration-strategy)
    - [9.1 Tool](#91-tool)
    - [9.2 Workflow](#92-workflow)
    - [9.3 Rules](#93-rules)
  - [10. Backup and Recovery](#10-backup-and-recovery)
    - [10.1 Backup Schedule](#101-backup-schedule)
    - [10.2 Backup Method](#102-backup-method)
    - [10.3 Recovery Process](#103-recovery-process)
    - [10.4 Data Retention Policy](#104-data-retention-policy)
  - [11. Revision History](#11-revision-history)

---

## 1. Introduction

This document defines the complete database schema for FinTrack AI, including all tables, columns, data types, constraints, indexes, relationships, and seeding requirements. It serves as the authoritative reference for database setup, migration authoring, and query optimization.

---

## 2. Database Technology

**DBMS:** PostgreSQL 15+

PostgreSQL is selected for the following reasons:

- Full ACID compliance, essential for financial data integrity
- Native UUID support via the pgcrypto extension
- Strong support for decimal arithmetic without floating-point errors
- Rich indexing options including partial and expression indexes
- Robust support for date and timestamp operations required for period-based reporting
- Mature ecosystem with Spring Data JPA integration

**ORM:** Spring Data JPA (Hibernate)

**Cache:** Redis 7+

Redis is used for:
- Session management and refresh token storage
- Caching frequent query results (e.g. dashboard summaries)
- Rate limiting metadata
- Temporary storage for AI processing tasks

**Extensions Required:**

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

---

## 3. Entity Relationship Overview

```
users
  |
  |-- 1:N --> transactions
  |-- 1:N --> categories (custom only; system categories have user_id NULL)
  |-- 1:N --> budgets
  |-- 1:N --> refresh_tokens
  |-- 1:N --> password_reset_tokens
  |-- 1:N --> notifications

categories
  |-- 1:N --> transactions (via category_id)
  |-- 1:N --> budgets (via category_id)

transactions
  |-- N:1 --> users
  |-- N:1 --> categories

budgets
  |-- N:1 --> users
  |-- N:1 --> categories

refresh_tokens
  |-- N:1 --> users

password_reset_tokens
  |-- N:1 --> users

notifications
  |-- N:1 --> users
```

---

## 4. Table Definitions

### 4.1 users

Stores registered user accounts.

```sql
CREATE TABLE users (
  id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  name                      VARCHAR(100)  NOT NULL,
  email                     VARCHAR(255)  NOT NULL UNIQUE,
  password_hash             VARCHAR(255)  NOT NULL,
  currency                  CHAR(3)       NOT NULL DEFAULT 'INR',
  email_verified            BOOLEAN       NOT NULL DEFAULT FALSE,
  verification_token        VARCHAR(64)   NULL,
  verification_token_expiry TIMESTAMPTZ   NULL,
  is_active                 BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | No | Primary key, auto-generated |
| name | VARCHAR(100) | No | Full name of the user |
| email | VARCHAR(255) | No | Unique email address |
| password_hash | VARCHAR(255) | No | bcrypt hash of password |
| currency | CHAR(3) | No | ISO 4217 currency code, default INR |
| email_verified | BOOLEAN | No | Whether the email has been confirmed |
| verification_token | VARCHAR(64) | Yes | Token sent for email verification |
| verification_token_expiry | TIMESTAMPTZ | Yes | Expiry of the verification token |
| is_active | BOOLEAN | No | Soft-delete flag |
| created_at | TIMESTAMPTZ | No | Record creation timestamp |
| updated_at | TIMESTAMPTZ | No | Last update timestamp |

### 4.2 refresh_tokens

Stores hashed refresh tokens for maintaining user sessions.

```sql
CREATE TABLE refresh_tokens (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | No | Primary key |
| user_id | UUID | No | FK to users |
| token_hash | VARCHAR(64) | No | SHA-256 hash of the refresh token |
| expires_at | TIMESTAMPTZ | No | Token expiration timestamp |
| created_at | TIMESTAMPTZ | No | Record creation timestamp |

### 4.3 password_reset_tokens

Stores time-limited tokens for password reset flows.

```sql
CREATE TABLE password_reset_tokens (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ NOT NULL,
  used        BOOLEAN     NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | No | Primary key |
| user_id | UUID | No | FK to users |
| token_hash | VARCHAR(64) | No | Hashed reset token |
| expires_at | TIMESTAMPTZ | No | Expiry (1 hour from creation) |
| used | BOOLEAN | No | Whether the token has been consumed |
| created_at | TIMESTAMPTZ | No | Record creation timestamp |

### 4.4 categories

Stores both system-default and user-created transaction categories.

```sql
CREATE TABLE categories (
  id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID          NULL REFERENCES users(id) ON DELETE CASCADE,
  name        VARCHAR(100)  NOT NULL,
  is_default  BOOLEAN       NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  CONSTRAINT  uq_category_name_per_user UNIQUE (user_id, name)
);
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | No | Primary key |
| user_id | UUID | Yes | NULL for system-default categories |
| name | VARCHAR(100) | No | Category name |
| is_default | BOOLEAN | No | TRUE for system categories |
| created_at | TIMESTAMPTZ | No | Record creation timestamp |
| updated_at | TIMESTAMPTZ | No | Last update timestamp |

Note: The unique constraint on (user_id, name) allows NULL user_id to appear multiple times (PostgreSQL treats NULL as distinct in unique constraints), which is correct behavior since system categories have NULL user_id and their uniqueness is enforced separately at the application layer.

### 4.5 transactions

The central table storing all financial transactions per user.

```sql
CREATE TABLE transactions (
  id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category_id  UUID          NOT NULL REFERENCES categories(id),
  amount       NUMERIC(15,2) NOT NULL CHECK (amount > 0),
  type         VARCHAR(10)   NOT NULL CHECK (type IN ('income', 'expense')),
  description  VARCHAR(255)  NULL,
  date         DATE          NOT NULL,
  created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | No | Primary key |
| user_id | UUID | No | FK to users |
| category_id | UUID | No | FK to categories |
| amount | NUMERIC(15,2) | No | Transaction amount, positive only |
| type | VARCHAR(10) | No | Either "income" or "expense" |
| description | VARCHAR(255) | Yes | Optional text description |
| date | DATE | No | Date of the transaction |
| created_at | TIMESTAMPTZ | No | Record creation timestamp |
| updated_at | TIMESTAMPTZ | No | Last update timestamp |

NUMERIC(15,2) is used instead of FLOAT to avoid floating-point precision errors in financial arithmetic.

### 4.6 budgets

Stores monthly budget limits set by users per category.

```sql
CREATE TABLE budgets (
  id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category_id   UUID          NOT NULL REFERENCES categories(id),
  limit_amount  NUMERIC(15,2) NOT NULL CHECK (limit_amount > 0),
  month         SMALLINT      NOT NULL CHECK (month BETWEEN 1 AND 12),
  year          SMALLINT      NOT NULL CHECK (year BETWEEN 2000 AND 2100),
  created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  CONSTRAINT    uq_budget_user_category_period UNIQUE (user_id, category_id, month, year)
);
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | No | Primary key |
| user_id | UUID | No | FK to users |
| category_id | UUID | No | FK to categories |
| limit_amount | NUMERIC(15,2) | No | Monthly spending limit |
| month | SMALLINT | No | Month number (1-12) |
| year | SMALLINT | No | Calendar year |
| created_at | TIMESTAMPTZ | No | Record creation timestamp |
| updated_at | TIMESTAMPTZ | No | Last update timestamp |

### 4.7 notifications

Stores in-app notification records per user.

```sql
CREATE TABLE notifications (
  id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type        VARCHAR(50)   NOT NULL,
  title       VARCHAR(255)  NOT NULL,
  body        TEXT          NOT NULL,
  is_read     BOOLEAN       NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | No | Primary key |
| user_id | UUID | No | FK to users |
| type | VARCHAR(50) | No | e.g. "budget_warning", "budget_exceeded" |
| title | VARCHAR(255) | No | Short notification title |
| body | TEXT | No | Full notification message |
| is_read | BOOLEAN | No | Whether the user has seen this notification |
| created_at | TIMESTAMPTZ | No | Record creation timestamp |

---

## 5. Relationships and Foreign Keys

| Relationship | Parent Table | Child Table | FK Column | On Delete |
|-------------|-------------|-------------|-----------|-----------|
| User -> Transactions | users | transactions | user_id | CASCADE |
| User -> Categories (custom) | users | categories | user_id | CASCADE |
| User -> Budgets | users | budgets | user_id | CASCADE |
| User -> Refresh Tokens | users | refresh_tokens | user_id | CASCADE |
| User -> Password Reset Tokens | users | password_reset_tokens | user_id | CASCADE |
| User -> Notifications | users | notifications | user_id | CASCADE |
| Category -> Transactions | categories | transactions | category_id | RESTRICT |
| Category -> Budgets | categories | budgets | category_id | RESTRICT |

The RESTRICT policy on category_id deletion in transactions and budgets is enforced at the application layer: when a user deletes a category, the service first reassigns all transactions to "Uncategorized" before the category record is deleted. This prevents orphaned records.

---

## 6. Indexes

### 6.1 Primary Key Indexes (Automatic)

PostgreSQL automatically creates a unique index for every PRIMARY KEY constraint.

### 6.2 Application Indexes

```sql
-- Transactions: most frequent query pattern is by user and date
CREATE INDEX idx_transactions_user_date
  ON transactions(user_id, date DESC);

-- Transactions: filtering by type
CREATE INDEX idx_transactions_user_type
  ON transactions(user_id, type);

-- Transactions: filtering by category
CREATE INDEX idx_transactions_user_category
  ON transactions(user_id, category_id);

-- Transactions: full text search on description (trigram index)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_transactions_description_trgm
  ON transactions USING GIN (description gin_trgm_ops);

-- Budgets: lookup by user and period
CREATE INDEX idx_budgets_user_period
  ON budgets(user_id, year, month);

-- Notifications: unread notifications per user
CREATE INDEX idx_notifications_user_unread
  ON notifications(user_id, is_read)
  WHERE is_read = FALSE;

-- Refresh tokens: lookup by hash
CREATE UNIQUE INDEX idx_refresh_tokens_hash
  ON refresh_tokens(token_hash);

-- Categories: user category lookup
CREATE INDEX idx_categories_user
  ON categories(user_id);
```

### 6.3 Index Strategy Notes

The transactions table is the most queried table and benefits most from compound indexes. The index on (user_id, date DESC) serves the default sorted list view. The GIN trigram index on description supports the ILIKE keyword search without a full table scan. All other indexes are single-column or simple compounds to avoid excessive write overhead.

---

## 7. Constraints and Business Rules

| Rule | Enforcement Location | Details |
|------|---------------------|---------|
| Positive transaction amounts | DB CHECK constraint | amount > 0 |
| Valid transaction type | DB CHECK constraint | IN ('income', 'expense') |
| Valid budget month | DB CHECK constraint | BETWEEN 1 AND 12 |
| One budget per user/category/period | DB UNIQUE constraint | (user_id, category_id, month, year) |
| Unique email per user | DB UNIQUE constraint | email column |
| Unique category name per user | DB UNIQUE constraint | (user_id, name) |
| Category ownership on transaction | Application layer | Service validates before INSERT |
| System category not deletable | Application layer | Checked via is_default flag |
| Budget threshold alerts | Application layer | Triggered after each transaction write |

---

## 8. Seed Data

The following system-default categories must be inserted during initial database setup.

```sql
INSERT INTO categories (id, user_id, name, is_default) VALUES
  (gen_random_uuid(), NULL, 'Food',            TRUE),
  (gen_random_uuid(), NULL, 'Transportation',  TRUE),
  (gen_random_uuid(), NULL, 'Utilities',       TRUE),
  (gen_random_uuid(), NULL, 'Entertainment',   TRUE),
  (gen_random_uuid(), NULL, 'Healthcare',      TRUE),
  (gen_random_uuid(), NULL, 'Education',       TRUE),
  (gen_random_uuid(), NULL, 'Shopping',        TRUE),
  (gen_random_uuid(), NULL, 'Salary',          TRUE),
  (gen_random_uuid(), NULL, 'Freelance',       TRUE),
  (gen_random_uuid(), NULL, 'Investment',      TRUE),
  (gen_random_uuid(), NULL, 'Uncategorized',   TRUE);
```

The "Uncategorized" category is reserved as the reassignment target when a user deletes a custom category. Its ID is looked up by name in the application layer.

---

## 9. Migration Strategy

### 9.1 Tool

Database schema migrations are managed using Flyway.

### 9.2 Workflow

All schema changes follow this process:

1. Developer creates a new SQL migration file in `src/main/resources/db/migration/`.
2. File naming follows the Flyway pattern: `V<version>__<description>.sql` (e.g. `V1__init_schema.sql`).
3. The migration file is committed to version control alongside the code change.
4. On application startup, Flyway automatically detects and applies pending migrations.
5. In production, migrations are applied by the application container or a dedicated migration job.

### 9.3 Rules

- Migrations are never modified after being applied to any environment.
- Destructive operations require a separate versioned migration.
- Seed data for development is handled via a separate `V999__seed_data.sql` or a Spring profile-specific `data.sql` file.

---

## 10. Backup and Recovery

### 10.1 Backup Schedule

- Automated full database backups are performed daily at 02:00 UTC.
- Backups are retained for 30 days.
- Backups are stored in a separate cloud storage bucket from the primary application.

### 10.2 Backup Method

PostgreSQL pg_dump is used to generate compressed backups:

```bash
pg_dump -Fc -h $DB_HOST -U $DB_USER -d $DB_NAME -f backup_$(date +%Y%m%d).dump
```

### 10.3 Recovery Process

In the event of data loss:

1. Stop the application server to prevent new writes.
2. Restore from the most recent backup using pg_restore.
3. Verify data integrity by checking transaction counts and user record counts.
4. Resume the application server.

### 10.4 Data Retention Policy

Per SRS Section 7.2:

- User transaction records are retained for the lifetime of the user account.
- On account deletion, all user data is marked for deletion and purged within 30 days via a scheduled cleanup job.
- System logs are retained for 90 days.

---

## 11. Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | March 28, 2026 | Project Team | Initial draft |