# Deployment Guide

## FinTrack AI - Intelligent Personal Finance Management System

**Version:** 1.0  
**Date:** March 28, 2026  
**Status:** Draft  
**Reference:** SRS v1.0, HLD v1.0, Security v1.0

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Environments](#2-environments)
3. [Prerequisites](#3-prerequisites)
4. [Project Structure](#4-project-structure)
5. [Environment Variables](#5-environment-variables)
6. [Docker Configuration](#6-docker-configuration)
7. [Database Setup and Migration](#7-database-setup-and-migration)
8. [Local Development Setup](#8-local-development-setup)
9. [Production Deployment](#9-production-deployment)
10. [Nginx Configuration](#10-nginx-configuration)
11. [CI/CD Pipeline](#11-cicd-pipeline)
12. [Monitoring and Health Checks](#12-monitoring-and-health-checks)
13. [Backup Procedure](#13-backup-procedure)
14. [Rollback Procedure](#14-rollback-procedure)
15. [Revision History](#15-revision-history)

---

## 1. Introduction

This document provides complete instructions for deploying FinTrack AI across development, staging, and production environments. It covers infrastructure setup, Docker configuration, environment variable management, database initialization, the CI/CD pipeline, and operational procedures including backup and rollback.

---

## 2. Environments

| Environment | Purpose | Access |
|-------------|---------|--------|
| Development | Local developer machines; feature development and unit testing | Developer only |
| Staging | Pre-production environment; integration and UAT testing | Internal team |
| Production | Live system serving end users | End users via public domain |

All environments use Docker Compose for service orchestration. Production additionally uses Nginx as a reverse proxy and TLS termination point.

---

## 3. Prerequisites

### 3.1 Local Development

- Docker Desktop 24.x or later
- Docker Compose v2.x or later
- Java JDK 17 or 21 (Temurin recommended)
- Node.js 20.x LTS (for Frontend)
- Maven 3.9.x or Gradle 8.x
- Git
- Ollama (Local LLM runner)

### 3.2 Production Server

- Ubuntu 22.04 LTS
- Docker Engine 24.x
- Docker Compose v2.x
- Nginx 1.24.x
- Certbot (for Let's Encrypt TLS certificates)
- Git
- A registered domain name with DNS A record pointing to the server IP

### 3.3 Accounts and Services Required

- Cloud VM provider account (AWS, GCP, Azure, or DigitalOcean)
- SMTP provider account (SendGrid, Mailgun, or similar)
- GitHub account (for CI/CD via GitHub Actions)

---

## 4. Project Structure

```
fintrack-ai/
  backend/                # Spring Boot Application
    src/main/java/        # Java source code
    src/main/resources/
      db/migration/       # Flyway migration files
      application.yml     # Spring configuration
    Dockerfile
    pom.xml               # Maven configuration
    .env.example
  frontend/               # React + Vite Application
    src/                  # React source
    public/
    Dockerfile
    nginx.conf            # Frontend Nginx config (serves static files)
    package.json
    .env.example
  desktop/                # JavaFX Application
    src/main/java/
    pom.xml
  mobile/                 # Android Application
    app/
    build.gradle
  docker-compose.yml      # Development compose file
  docker-compose.prod.yml # Production compose file
  nginx/
    nginx.conf            # Production reverse proxy config
    ssl/                  # TLS certificate files (not in version control)
  .github/
    workflows/
      deploy.yml          # CI/CD pipeline
  .gitignore
  README.md
```

---

## 5. Environment Variables

### 5.1 Backend Environment Variables

Copy `backend/.env.example` to `backend/.env` or configure via `application.yml`.

```
# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=5000
BASE_URL=https://fintrack.ai

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://fintrack-db:5432/fintrack_db
SPRING_DATASOURCE_USERNAME=fintrack_user
SPRING_DATASOURCE_PASSWORD=strongpassword

# JWT
JWT_SECRET=<minimum 256-bit random string>
JWT_EXPIRY=3600000
REFRESH_TOKEN_EXPIRY=604800000

# Redis
SPRING_DATA_REDIS_HOST=fintrack-redis
SPRING_DATA_REDIS_PORT=6379

# Email (SMTP)
SPRING_MAIL_HOST=smtp.sendgrid.net
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=apikey
SPRING_MAIL_PASSWORD=<sendgrid api key>
EMAIL_FROM=noreply@fintrack.ai

# AI (Ollama Local)
OLLAMA_BASE_URL=http://fintrack-ollama:11434
AI_MODEL=llama3:8b

# Rate Limiting
BUCKET4J_ENABLED=true
```

### 5.2 Frontend Environment Variables

Copy `frontend/.env.example` to `frontend/.env`.

```
VITE_API_BASE_URL=https://api.fintrack.ai/api/v1
VITE_SOCKET_URL=wss://api.fintrack.ai/ws
```

### 5.3 Generating Secure Secrets

Use a cryptographically secure generator:

```bash
# Using openssl
openssl rand -hex 64
```

---

## 6. Docker Configuration

### 6.1 Backend Dockerfile

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/target/*.jar app.jar
USER appuser
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 6.2 Frontend Dockerfile

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.24-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 6.3 docker-compose.yml (Development)

```yaml
version: '3.9'

services:
  backend:
    build: ./backend
    container_name: fintrack-backend
    env_file: ./backend/.env
    ports:
      - "5000:5000"
    depends_on:
      - db
      - redis
      - ollama
    networks:
      - fintrack-net

  frontend:
    build: ./frontend
    container_name: fintrack-frontend
    env_file: ./frontend/.env
    ports:
      - "3000:80"
    networks:
      - fintrack-net

  db:
    image: postgres:15-alpine
    container_name: fintrack-db
    environment:
      POSTGRES_USER: fintrack_user
      POSTGRES_PASSWORD: strongpassword
      POSTGRES_DB: fintrack_db
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    networks:
      - fintrack-net

  redis:
    image: redis:7-alpine
    container_name: fintrack-redis
    ports:
      - "6379:6379"
    networks:
      - fintrack-net

  ollama:
    image: ollama/ollama:latest
    container_name: fintrack-ollama
    volumes:
      - ollama-data:/root/.ollama
    ports:
      - "11434:11434"
    networks:
      - fintrack-net

volumes:
  pgdata:
  ollama-data:

networks:
  fintrack-net:
    driver: bridge
```

### 6.4 docker-compose.prod.yml (Production)

```yaml
version: '3.9'

services:
  backend:
    image: ghcr.io/your-org/fintrack-backend:latest
    container_name: fintrack-backend
    env_file: /etc/fintrack/backend.env
    restart: always
    depends_on:
      - db
      - redis
    networks:
      - fintrack-net

  frontend:
    image: ghcr.io/your-org/fintrack-frontend:latest
    container_name: fintrack-frontend
    restart: always
    networks:
      - fintrack-net

  db:
    image: postgres:15-alpine
    container_name: fintrack-db
    env_file: /etc/fintrack/db.env
    restart: always
    volumes:
      - pgdata:/var/lib/postgresql/data
    networks:
      - fintrack-net

  redis:
    image: redis:7-alpine
    container_name: fintrack-redis
    restart: always
    networks:
      - fintrack-net

volumes:
  pgdata:

networks:
  fintrack-net:
    driver: bridge
```

Note: In production, env files are stored at /etc/fintrack/ on the server with permissions 600 owned by root. Ports are not exposed directly; Nginx handles external traffic.

---

## 7. Database Setup and Migration

### 7.1 Initial Setup

Flyway handles migrations automatically on application startup. To trigger the first migration:

```bash
# 1. Start all infrastructure
docker compose -f docker-compose.prod.yml up -d db redis ollama

# 2. Start the backend (Flyway runs on start)
docker compose -f docker-compose.prod.yml up -d backend
```

### 7.2 Applying Migrations on Updates

Flyway is integrated into the Spring Boot lifecycle. When a new version of the backend container starts, it automatically checks for and applies new SQL migration files found in `classpath:db/migration`.

### 7.3 Rollback a Migration

Flyway Community Edition does not support automatic rollback. To revert:

1. Create a new "Undo" migration script (e.g. `V1.1__undo_previous.sql`) that reverses the changes.
2. Apply it as a regular forward migration.
3. Alternatively, restore the database from a backup before the migration was applied.

---

## 8. Local Development Setup

```bash
# 1. Clone the repository
git clone https://github.com/your-org/fintrack-ai.git
cd fintrack-ai

# 2. Create environment files from examples
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
# Edit both files with your local values

# 3. Start all services
docker compose up -d

# 4. Pull and run the AI model in Ollama
docker exec -it fintrack-ollama ollama run llama3:8b

# 5. Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:5000/api/v1
```

To stop all services:

```bash
docker compose down
```

To reset the database (drops all data):

```bash
docker compose down -v
docker compose up -d
docker compose exec backend npx prisma migrate dev
docker compose exec backend npx prisma db seed
```

---

## 9. Production Deployment

### 9.1 First-Time Server Setup

```bash
# 1. Update system packages
sudo apt update && sudo apt upgrade -y

# 2. Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 3. Install Docker Compose v2
sudo apt install docker-compose-plugin -y

# 4. Install Nginx and Certbot
sudo apt install nginx certbot python3-certbot-nginx -y

# 5. Create environment file directory
sudo mkdir -p /etc/fintrack
sudo chmod 700 /etc/fintrack

# 6. Create and populate environment files
sudo nano /etc/fintrack/backend.env   # Paste backend env vars
sudo nano /etc/fintrack/db.env        # Paste DB env vars
sudo chmod 600 /etc/fintrack/*.env

# 7. Clone the repository
git clone https://github.com/your-org/fintrack-ai.git /opt/fintrack
```

### 9.2 Obtain TLS Certificate

```bash
sudo certbot --nginx -d fintrack.ai -d api.fintrack.ai
```

Certbot will automatically configure Nginx and set up automatic renewal.

### 9.3 Deploy Application

```bash
cd /opt/fintrack

# Pull latest images (built by CI/CD)
docker compose -f docker-compose.prod.yml pull

# Start services
docker compose -f docker-compose.prod.yml up -d

# Run migrations
docker compose -f docker-compose.prod.yml run --rm backend \
  npx prisma migrate deploy

# Verify all containers are running
docker compose -f docker-compose.prod.yml ps
```

### 9.4 Subsequent Deployments

```bash
cd /opt/fintrack

# Pull latest code (if deploying from source) or pull latest images
docker compose -f docker-compose.prod.yml pull

# Restart services with zero-downtime approach:
# 1. Start new containers
docker compose -f docker-compose.prod.yml up -d --no-deps backend frontend

# 2. Run migrations if schema changed
docker compose -f docker-compose.prod.yml run --rm backend \
  npx prisma migrate deploy

# 3. Verify health
curl -f https://api.fintrack.ai/health
```

---

## 10. Nginx Configuration

The production Nginx configuration handles TLS termination, routing to frontend and backend containers, and security headers.

```nginx
# /etc/nginx/sites-available/fintrack

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name fintrack.ai api.fintrack.ai;
    return 301 https://$host$request_uri;
}

# Frontend
server {
    listen 443 ssl http2;
    server_name fintrack.ai;

    ssl_certificate     /etc/letsencrypt/live/fintrack.ai/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/fintrack.ai/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy no-referrer always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';" always;

    location / {
        proxy_pass http://fintrack-frontend:80;
        proxy_set_header Host $host;
    }
}

# Backend API
server {
    listen 443 ssl http2;
    server_name api.fintrack.ai;

    ssl_certificate     /etc/letsencrypt/live/fintrack.ai/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/fintrack.ai/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;

    # WebSocket support
    location /socket.io/ {
        proxy_pass http://fintrack-backend:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    location / {
        proxy_pass http://fintrack-backend:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 1m;
    }
}
```

Enable the site and reload Nginx:

```bash
sudo ln -s /etc/nginx/sites-available/fintrack /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## 11. CI/CD Pipeline

The CI/CD pipeline is implemented using GitHub Actions. It runs on every push to the main branch.

### 11.1 Pipeline Stages

```
Push to main
    |
    v
[ 1. Test ]
    Run unit and integration tests
    Run npm audit (fail on high/critical)
    |
    v (on success)
[ 2. Build ]
    Build Docker images for backend and frontend
    Tag images with commit SHA and "latest"
    Push to GitHub Container Registry (ghcr.io)
    |
    v (on success)
[ 3. Deploy ]
    SSH into production server
    Pull latest images
    Run docker compose up -d
    Run prisma migrate deploy
    Run health check on /health endpoint
    Send deployment notification to team Slack channel
```

### 11.2 GitHub Actions Workflow

```yaml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - name: Install dependencies
        run: cd backend && npm ci
      - name: Run audit
        run: cd backend && npm audit --audit-level=high
      - name: Run tests
        run: cd backend && npm test

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Build and push backend
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: |
            ghcr.io/${{ github.repository }}/fintrack-backend:latest
            ghcr.io/${{ github.repository }}/fintrack-backend:${{ github.sha }}
      - name: Build and push frontend
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          push: true
          tags: |
            ghcr.io/${{ github.repository }}/fintrack-frontend:latest
            ghcr.io/${{ github.repository }}/fintrack-frontend:${{ github.sha }}

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to production
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/fintrack
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d
            docker compose -f docker-compose.prod.yml run --rm backend \
              npx prisma migrate deploy
            curl -f https://api.fintrack.ai/health || exit 1
            echo "Deployment successful"
```

### 11.3 GitHub Secrets Required

| Secret | Value |
|--------|-------|
| PROD_HOST | Production server IP address |
| PROD_USER | SSH username on production server |
| PROD_SSH_KEY | Private SSH key for deployment user |

---

## 12. Monitoring and Health Checks

### 12.1 Health Check Endpoint

The backend exposes a health check endpoint that the CI/CD pipeline and load balancer can poll:

**GET /health**

Response 200:
```json
{
  "status": "ok",
  "timestamp": "2026-03-28T12:00:00Z",
  "services": {
    "database": "ok",
    "redis": "ok"
  }
}
```

If the database or Redis is unreachable, the response is HTTP 503.

### 12.2 Container Health Checks

Docker Compose healthcheck is configured for the backend service:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:5000/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 30s
```

### 12.3 Log Access

View real-time logs:

```bash
# All services
docker compose -f docker-compose.prod.yml logs -f

# Backend only
docker compose -f docker-compose.prod.yml logs -f backend

# Last 100 lines
docker compose -f docker-compose.prod.yml logs --tail=100 backend
```

---

## 13. Backup Procedure

### 13.1 Automated Daily Backup

A cron job on the production server runs daily at 02:00 UTC:

```bash
# /etc/cron.d/fintrack-backup
0 2 * * * root /opt/fintrack/scripts/backup.sh >> /var/log/fintrack-backup.log 2>&1
```

**backup.sh:**

```bash
#!/bin/bash
set -e

DATE=$(date +%Y%m%d)
BACKUP_DIR=/var/backups/fintrack
BACKUP_FILE="${BACKUP_DIR}/fintrack_db_${DATE}.dump"

mkdir -p "$BACKUP_DIR"

docker compose -f /opt/fintrack/docker-compose.prod.yml exec -T db \
  pg_dump -Fc -U fintrack_user fintrack_db > "$BACKUP_FILE"

echo "[$(date -u)] Backup created: $BACKUP_FILE ($(du -h $BACKUP_FILE | cut -f1))"

# Delete backups older than 30 days
find "$BACKUP_DIR" -name "*.dump" -mtime +30 -delete
echo "[$(date -u)] Old backups cleaned."
```

### 13.2 Manual Backup

```bash
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -Fc -U fintrack_user fintrack_db > /var/backups/fintrack/manual_$(date +%Y%m%d_%H%M%S).dump
```

---

## 14. Rollback Procedure

### 14.1 Application Rollback

To roll back to a previous version using a specific image tag:

```bash
cd /opt/fintrack

# Edit docker-compose.prod.yml to pin image tags to the previous commit SHA
# Replace "latest" with the previous known-good SHA

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

If the rollback involves a schema migration, follow the manual migration rollback procedure described in Section 7.3 before restarting the application containers.

### 14.2 Database Rollback from Backup

Use only when data corruption or accidental deletion has occurred.

```bash
# 1. Stop application containers to prevent new writes
docker compose -f docker-compose.prod.yml stop backend

# 2. Drop and recreate the database
docker compose -f docker-compose.prod.yml exec db \
  psql -U postgres -c "DROP DATABASE fintrack_db;"
docker compose -f docker-compose.prod.yml exec db \
  psql -U postgres -c "CREATE DATABASE fintrack_db OWNER fintrack_user;"

# 3. Restore from backup file
docker compose -f docker-compose.prod.yml exec -T db \
  pg_restore -U fintrack_user -d fintrack_db < /var/backups/fintrack/fintrack_db_YYYYMMDD.dump

# 4. Restart application
docker compose -f docker-compose.prod.yml start backend
```

---

## 15. Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | March 28, 2026 | Project Team | Initial draft |