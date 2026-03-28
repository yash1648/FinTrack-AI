# Software Requirements Specification

## FinTrack AI - Intelligent Personal Finance Management System

**Version:** 1.0  
**Date:** March 28, 2026  
**Status:** Approved

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Overall Description](#2-overall-description)
3. [System Features and Functional Requirements](#3-system-features-and-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [External Interface Requirements](#5-external-interface-requirements)
6. [System Architecture](#6-system-architecture)
7. [Data Requirements](#7-data-requirements)
8. [Constraints and Assumptions](#8-constraints-and-assumptions)
9. [Appendix](#9-appendix)

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) document defines the functional and non-functional requirements for FinTrack AI, an intelligent personal financial management application. It is intended for use by developers, system designers, testers, and project stakeholders involved in the development and delivery of the system.

### 1.2 Scope

FinTrack AI is a multi-platform personal finance management system that enables users to track income and expenses, categorize transactions, and receive AI-driven insights. The system consists of a **React-based Web application**, a **JavaFX-based Desktop application**, and a **native Android Mobile application**, all served by a centralized **Spring Boot backend**.

The system integrates data visualization, local natural language processing (via Ollama), and intelligent analysis to help users make better financial decisions across all their devices.

The system covers the following core functional areas:

- Multi-platform transaction recording (Web, Desktop, Mobile)
- Expense categorization and custom category management
- AI-powered financial analysis and recommendations (Local LLM)
- Real-time budget monitoring and alerts (WebSocket)
- Financial data visualization and reporting

### 1.3 Definitions, Acronyms, and Abbreviations

| Term | Definition |
|------|------------|
| SRS | Software Requirements Specification |
| AI | Artificial Intelligence |
| NLP | Natural Language Processing |
| UI | User Interface |
| API | Application Programming Interface |
| DBMS | Database Management System |
| JWT | JSON Web Token |
| HTTPS | Hypertext Transfer Protocol Secure |
| INR | Indian National Rupee |
| FR | Functional Requirement |
| NFR | Non-Functional Requirement |

### 1.4 References

- IEEE Std 830-1998: IEEE Recommended Practice for Software Requirements Specifications
- OWASP Security Guidelines for Web Applications
- Project Abstract Document: FinTrack AI (Version 1.0)

### 1.5 Overview

Section 2 provides a general description of the product, its context, and user characteristics. Section 3 covers detailed functional requirements. Section 4 defines non-functional requirements. Sections 5 through 9 describe interface requirements, system architecture, data requirements, constraints, and appendices respectively.

---

## 2. Overall Description

### 2.1 Product Perspective

FinTrack AI is a cross-platform system built on a multi-tier architecture:

- **Web Frontend:** Built with React + Vite.
- **Desktop Application:** Built with JavaFX.
- **Mobile Application:** Built for Android.
- **Backend API:** Built with Spring Boot (Java 17/21).
- **Database:** PostgreSQL for persistence.
- **Cache:** Redis for low-latency session and AI caching.
- **AI Engine:** Local LLM via Ollama.

The backend acts as the single source of truth, managing all business logic and orchestrating services across all clients.

### 2.2 Product Functions

The primary functions of FinTrack AI are:

- User registration, authentication, and profile management
- Recording of income and expense transactions
- Categorization of transactions into predefined and custom categories
- Natural language input for adding transactions
- AI-based analysis of spending patterns and financial behavior
- Detection of unusual or anomalous expenditures
- Budget creation, tracking, and alert generation
- Visual financial reporting through charts and dashboards
- Generation of monthly and category-level financial summaries

### 2.3 User Classes and Characteristics

**Primary User - Individual End User**

The primary users are individuals who wish to manage their personal finances. These users are expected to have basic familiarity with web applications and mobile devices. They may not have any background in finance or accounting. The interface must be simple, intuitive, and require minimal technical knowledge.

**Secondary User - System Administrator**

Administrators are responsible for managing the platform, maintaining user accounts, monitoring system health, and configuring system-wide settings. Administrators require access to backend tools and logs and are expected to have technical proficiency.

### 2.4 Operating Environment

- **Web:** Compatible with Google Chrome, Mozilla Firefox, Microsoft Edge, and Safari.
- **Desktop:** Compatible with Windows, macOS, and Linux (via JavaFX).
- **Mobile:** Compatible with Android 8.0 (API level 26) and above.
- **Backend Server:** Linux-based environments (Ubuntu 22.04 LTS recommended) using Docker.
- **Database:** PostgreSQL 15+ and Redis 7+.
- **AI Engine:** Ollama (requires local GPU or sufficient CPU/RAM for inference).

### 2.5 Design and Implementation Constraints

- The system must use HTTPS for all client-server communication.
- Real-time updates must be handled via WebSocket (WSS).
- User passwords must be stored in hashed form using bcrypt.
- AI processing must be performed locally using Ollama to ensure data privacy.
- The backend must be built using Spring Boot to ensure scalability and enterprise-grade security.

### 2.6 Assumptions and Dependencies

- Users are assumed to have stable internet connectivity for real-time features.
- The system assumes currency inputs will primarily be in INR; multi-currency support is a future consideration.
- AI-based features depend on the availability of sufficient transaction history for meaningful analysis.
- Third-party AI model APIs, if used, are assumed to be available and reliable.

---

## 3. System Features and Functional Requirements

### 3.1 User Registration and Authentication

**Description:** The system shall allow users to create accounts and securely log in.

**Functional Requirements:**

- FR-1.1: The system shall allow new users to register using an email address and password.
- FR-1.2: The system shall validate email format and enforce a minimum password strength policy during registration.
- FR-1.3: The system shall send a verification email upon registration and require email confirmation before granting access.
- FR-1.4: The system shall allow registered users to log in using their email and password.
- FR-1.5: The system shall issue a session token (JWT or equivalent) upon successful login.
- FR-1.6: The system shall support password reset via a time-limited email link.
- FR-1.7: The system shall allow users to update their profile information including name and currency preferences.
- FR-1.8: The system shall automatically log out users after a configurable period of inactivity.

### 3.2 Transaction Management

**Description:** The system shall allow users to record, view, edit, and delete financial transactions.

**Functional Requirements:**

- FR-2.1: The system shall allow users to add a transaction with the following fields: amount, date, description, type (income or expense), and category.
- FR-2.2: The system shall allow users to edit any field of an existing transaction.
- FR-2.3: The system shall allow users to delete a transaction, with a confirmation prompt before deletion.
- FR-2.4: The system shall display a paginated transaction history list sorted by date in descending order by default.
- FR-2.5: The system shall allow users to filter transactions by date range, category, type, and amount range.
- FR-2.6: The system shall allow users to search transactions by keyword in the description field.
- FR-2.7: The system shall display the current month's total income, total expenses, and net balance on the main dashboard.

### 3.3 Expense Categorization

**Description:** The system shall organize transactions into categories to facilitate analysis and reporting.

**Functional Requirements:**

- FR-3.1: The system shall provide a set of predefined expense categories including, but not limited to: Food, Transportation, Utilities, Entertainment, Healthcare, Education, Shopping, and Other.
- FR-3.2: The system shall allow users to create custom categories in addition to the predefined ones.
- FR-3.3: The system shall allow users to rename or delete custom categories.
- FR-3.4: The system shall reassign transactions in a deleted category to an "Uncategorized" default category.
- FR-3.5: The system shall allow a single category to be assigned per transaction.

### 3.4 Natural Language Transaction Input

**Description:** The system shall allow users to add transactions using plain text commands.

**Functional Requirements:**

- FR-4.1: The system shall accept natural language input such as "Spent Rs. 500 on groceries" and parse it into a structured transaction.
- FR-4.2: The system shall extract the transaction amount, category, and description from the natural language input.
- FR-4.3: The system shall display a confirmation screen with the parsed transaction details before saving, allowing the user to correct any field.
- FR-4.4: The system shall gracefully handle inputs it cannot parse and prompt the user to enter the transaction using the standard form.
- FR-4.5: The system shall support common variations in input phrasing for the same transaction type.

### 3.5 AI-Powered Financial Analysis

**Description:** The system shall analyze user transaction data and provide intelligent financial insights.

**Functional Requirements:**

- FR-5.1: The system shall identify and display the user's top spending categories for a given period.
- FR-5.2: The system shall detect and flag transactions that are significantly higher than the user's historical average for that category.
- FR-5.3: The system shall generate a monthly spending summary that compares the current month with the previous month.
- FR-5.4: The system shall provide personalized recommendations for reducing expenses based on observed spending patterns.
- FR-5.5: The system shall project an estimated end-of-month expenditure based on spending trends within the current month.
- FR-5.6: The system shall identify recurring transactions and notify the user of detected patterns.

### 3.6 Budget Management

**Description:** The system shall allow users to set spending limits per category and monitor adherence.

**Functional Requirements:**

- FR-6.1: The system shall allow users to create a budget by setting a monthly spending limit for one or more categories.
- FR-6.2: The system shall display a budget progress indicator for each category showing the amount spent versus the limit.
- FR-6.3: The system shall send an in-app alert when a user reaches 80% of a category budget.
- FR-6.4: The system shall send an in-app alert when a user exceeds a category budget.
- FR-6.5: The system shall allow users to edit or delete an existing budget.
- FR-6.6: The system shall reset budget tracking at the start of each calendar month.

### 3.7 Financial Reports and Visualizations

**Description:** The system shall present financial data in visual formats to support comprehension and decision-making.

**Functional Requirements:**

- FR-7.1: The system shall display a pie chart showing expense distribution across categories for a selected time period.
- FR-7.2: The system shall display a bar chart comparing monthly income and expenses over the past six months.
- FR-7.3: The system shall display a line chart showing cumulative spending over the current month.
- FR-7.4: The system shall allow users to select custom date ranges for report generation.
- FR-7.5: The system shall allow users to export transaction data as a CSV file.
- FR-7.6: The system shall display summary statistics including total income, total expenses, largest single expense, and average daily spending for the selected period.

---

## 4. Non-Functional Requirements

### 4.1 Performance Requirements

- NFR-1.1: The system shall load the main dashboard within 3 seconds under normal network conditions.
- NFR-1.2: Transaction search and filter queries shall return results within 2 seconds for datasets of up to 10,000 records per user.
- NFR-1.3: AI analysis results shall be generated and displayed within 5 seconds of the user requesting them.
- NFR-1.4: The system shall support at least 500 concurrent users without performance degradation.

### 4.2 Security Requirements

- NFR-2.1: All data transmitted between the client and server shall be encrypted using TLS 1.2 or higher.
- NFR-2.2: User passwords shall be stored using a cryptographic hashing algorithm (bcrypt with a minimum work factor of 12).
- NFR-2.3: The system shall implement protection against common web vulnerabilities including SQL injection, Cross-Site Scripting (XSS), and Cross-Site Request Forgery (CSRF).
- NFR-2.4: Session tokens shall expire after 24 hours of inactivity.
- NFR-2.5: Failed login attempts shall be rate-limited, with account lockout triggered after 5 consecutive failures within a 10-minute window.
- NFR-2.6: Sensitive financial data shall not be logged in application logs or error messages.

### 4.3 Usability Requirements

- NFR-3.1: A new user shall be able to add their first transaction within 3 minutes of creating an account, without requiring external documentation.
- NFR-3.2: All primary user actions shall be reachable within 3 navigational steps from the dashboard.
- NFR-3.3: The application shall display clear error messages with corrective guidance for all user input errors.
- NFR-3.4: The user interface shall be responsive and fully functional on screen widths from 375px (mobile) to 1920px (desktop).

### 4.4 Reliability Requirements

- NFR-4.1: The system shall maintain an uptime of at least 99% on a monthly basis.
- NFR-4.2: The system shall perform automated database backups at least once every 24 hours.
- NFR-4.3: In the event of a server error, the system shall display a user-friendly error message and preserve any unsaved transaction data where possible.

### 4.5 Maintainability Requirements

- NFR-5.1: The codebase shall follow consistent coding standards and include inline documentation for all major modules.
- NFR-5.2: The system shall use a modular architecture that allows independent update or replacement of the AI analysis component without affecting other system modules.
- NFR-5.3: The system shall include application logging with configurable log levels for monitoring and debugging.

### 4.6 Scalability Requirements

- NFR-6.1: The backend architecture shall support horizontal scaling to handle increased user load.
- NFR-6.2: The database design shall efficiently support per-user data volumes of up to 50,000 transactions without query performance degradation.

---

## 5. External Interface Requirements

### 5.1 User Interface

- The system shall provide a web-based graphical user interface accessible through a browser.
- The interface shall include a navigation bar, a dashboard home screen, a transaction entry form, a reports section, and a settings panel.
- The interface shall use consistent typography, color coding, and iconography across all screens.
- Form fields shall include inline validation with real-time feedback.

### 5.2 Hardware Interfaces

The application does not require any dedicated hardware interface. It shall function on any device with a standards-compliant web browser and internet access.

### 5.3 Software Interfaces

- **Web Frontend:** React + Vite, Axios for API calls, WebSocket client for realtime.
- **Desktop Application:** JavaFX (Java 17/21).
- **Mobile Application:** Native Android (Kotlin).
- **Backend API:** Spring Boot (Java), Spring Data JPA, Spring Security, Spring WebSocket.
- **Database:** PostgreSQL for relational data, Redis for caching.
- **AI Component:** Local Ollama API (localhost:11434).
- **Email Service:** Spring Mail (SMTP).

### 5.4 Communication Interfaces

- All client-server communication shall use HTTPS (TLS 1.2+).
- Realtime notifications shall use Secure WebSocket (WSS).
- API endpoints shall follow RESTful conventions.

---

## 6. System Architecture

### 6.1 Architectural Overview

FinTrack AI follows a three-tier architecture:

**Presentation Tier:** The frontend layer is responsible for rendering the user interface and handling user interactions. It communicates exclusively with the application tier via API.

**Application Tier:** The backend server manages business logic, authentication, transaction processing, AI analysis orchestration, and API request/response handling.

**Data Tier:** The database stores user accounts, transactions, categories, budget configurations, and application metadata.

### 6.2 Component Description

| Component | Responsibility |
|-----------|----------------|
| Authentication Module | User registration, login, token issuance, and session management |
| Transaction Service | CRUD operations for financial transactions |
| Category Service | Management of predefined and custom transaction categories |
| NLP Parser | Interpretation of natural language transaction input |
| AI Analysis Engine | Pattern detection, anomaly identification, and recommendation generation |
| Budget Manager | Budget configuration, progress tracking, and alert triggering |
| Reporting Engine | Aggregation and formatting of financial data for visualization |
| Notification Service | Delivery of in-app and email alerts |

---

## 7. Data Requirements

### 7.1 Data Entities

**User**

| Field | Type | Description |
|-------|------|-------------|
| user_id | UUID | Unique identifier |
| name | String | Full name of the user |
| email | String | Email address (unique) |
| password_hash | String | Hashed password |
| currency | String | Preferred currency code |
| created_at | Timestamp | Account creation date |

**Transaction**

| Field | Type | Description |
|-------|------|-------------|
| transaction_id | UUID | Unique identifier |
| user_id | UUID | Foreign key to User |
| amount | Decimal | Transaction amount |
| type | Enum | Income or Expense |
| category_id | UUID | Foreign key to Category |
| description | String | Text description |
| date | Date | Date of transaction |
| created_at | Timestamp | Record creation timestamp |

**Category**

| Field | Type | Description |
|-------|------|-------------|
| category_id | UUID | Unique identifier |
| user_id | UUID | Null for system defaults |
| name | String | Category name |
| is_default | Boolean | True for predefined categories |

**Budget**

| Field | Type | Description |
|-------|------|-------------|
| budget_id | UUID | Unique identifier |
| user_id | UUID | Foreign key to User |
| category_id | UUID | Foreign key to Category |
| limit_amount | Decimal | Monthly spending limit |
| month | Integer | Target month (1-12) |
| year | Integer | Target year |

### 7.2 Data Retention

- Transaction records shall be retained for the lifetime of the user account.
- Upon account deletion, all associated user data shall be permanently removed within 30 days.
- System logs shall be retained for a maximum of 90 days.

---

## 8. Constraints and Assumptions

### 8.1 Constraints

- The system must be developed within the timeline and resource allocations defined in the project plan.
- The initial release will support only INR as the primary currency.
- The AI recommendation engine requires a minimum of 30 days of transaction history to generate meaningful insights.
- External AI API usage, if applicable, is subject to the rate limits and pricing of the selected provider.

### 8.2 Assumptions

- Users will have access to a device with a modern web browser and active internet connectivity.
- Users are responsible for the accuracy of the transaction data they enter into the system.
- The system will not integrate with bank accounts or payment gateways in version 1.0.
- Administrative functions will be managed through direct database or server-level access in version 1.0, with a dedicated admin interface considered for future releases.

---

## 9. Appendix

### 9.1 Glossary

**Transaction:** A record of a single financial activity, either income received or an expense incurred.

**Category:** A label used to classify transactions into groups such as Food, Transport, or Utilities.

**Budget:** A user-defined monthly spending limit for a given expense category.

**Natural Language Input:** A method of entering data using plain conversational text rather than structured form fields.

**Anomaly Detection:** The process of identifying transactions that deviate significantly from established patterns in the user's financial history.

**Dashboard:** The primary screen of the application presenting a summary of the user's financial status at a glance.

### 9.2 Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | March 28, 2026 | Project Team | Approved    |