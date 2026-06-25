-- =============================================================================
-- V1: Initial schema for FinTrack AI
-- Compatible with H2 (PostgreSQL mode) and PostgreSQL
-- =============================================================================

-- ---------------
-- USERS
-- ---------------
CREATE TABLE users (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(255)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'INR',
    email_verified BOOLEAN    NOT NULL DEFAULT false,
    verification_token  VARCHAR(64),
    verification_token_expiry TIMESTAMP,
    is_active   BOOLEAN       NOT NULL DEFAULT true,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);

-- ---------------
-- REFRESH TOKENS
-- ---------------
CREATE TABLE refresh_tokens (
    id         UUID      NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);

-- ---------------
-- PASSWORD RESET TOKENS
-- ---------------
CREATE TABLE password_reset_tokens (
    id         UUID      NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used       BOOLEAN   NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_hash ON password_reset_tokens (token_hash);

-- ---------------
-- CATEGORIES
-- ---------------
CREATE TABLE categories (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID,
    name       VARCHAR(100) NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_category_name_per_user UNIQUE (user_id, name),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_categories_user ON categories (user_id);
CREATE INDEX idx_categories_is_default ON categories (is_default);

-- ---------------
-- TRANSACTIONS
-- ---------------
CREATE TABLE transactions (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL,
    category_id UUID            NOT NULL,
    amount      DECIMAL(15, 2)  NOT NULL CHECK (amount > 0),
    type        VARCHAR(10)     NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    description VARCHAR(255),
    date        DATE            NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_user     FOREIGN KEY (user_id)     REFERENCES users (id)        ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id)   ON DELETE RESTRICT
);

CREATE INDEX idx_transactions_user           ON transactions (user_id);
CREATE INDEX idx_transactions_date           ON transactions (date);
CREATE INDEX idx_transactions_user_date      ON transactions (user_id, date DESC);
CREATE INDEX idx_transactions_type           ON transactions (type);
CREATE INDEX idx_transactions_user_type      ON transactions (user_id, type);
CREATE INDEX idx_transactions_category       ON transactions (category_id);
CREATE INDEX idx_transactions_user_category  ON transactions (user_id, category_id);

-- Basic index on description for search (full-text search index is added below for PostgreSQL)
CREATE INDEX idx_transactions_description ON transactions (description);

-- ---------------
-- BUDGETS
-- ---------------
CREATE TABLE budgets (
    id           UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL,
    category_id  UUID           NOT NULL,
    limit_amount DECIMAL(15, 2) NOT NULL CHECK (limit_amount > 0),
    "month"      SMALLINT       NOT NULL CHECK ("month" BETWEEN 1 AND 12),
    "year"       SMALLINT       NOT NULL CHECK ("year" BETWEEN 2000 AND 2100),
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT uq_budget_user_category_period UNIQUE (user_id, category_id, "month", "year"),
    CONSTRAINT fk_budgets_user     FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_budgets_user         ON budgets (user_id);
CREATE INDEX idx_budgets_user_period  ON budgets (user_id, "year", "month");

-- ---------------
-- NOTIFICATIONS
-- ---------------
CREATE TABLE notifications (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user        ON notifications (user_id);
CREATE INDEX idx_notifications_is_read     ON notifications (is_read);
CREATE INDEX idx_notifications_user_read   ON notifications (user_id, is_read);

-- ---------------
-- SYSTEM DEFAULT CATEGORIES
-- Inserted after schema creation so user_id FK resolves (NULL = system category)
-- ---------------
INSERT INTO categories (user_id, name, is_default) VALUES
    (NULL, 'Food',            true),
    (NULL, 'Transportation',  true),
    (NULL, 'Utilities',       true),
    (NULL, 'Entertainment',   true),
    (NULL, 'Healthcare',      true),
    (NULL, 'Education',       true),
    (NULL, 'Shopping',        true),
    (NULL, 'Salary',          true),
    (NULL, 'Freelance',       true),
    (NULL, 'Investment',      true),
    (NULL, 'Gifts',           true),
    (NULL, 'Other',           true),
    (NULL, 'Uncategorized',   true);
