# =============================================================================
# FinTrack AI — Makefile
# Development, build, test, and deployment automation
# =============================================================================

SHELL := /bin/bash
.DEFAULT_GOAL := help
.ONESHELL:

# ─── Project Metadata ─────────────────────────────────────────────────────────
APP_NAME    := fintrack-ai
BACKEND_DIR := backend
FRONTEND_DIR := frontend

# ─── Colors ──────────────────────────────────────────────────────────────────
RED    := \033[0;31m
GREEN  := \033[0;32m
YELLOW := \033[0;33m
BLUE   := \033[0;34m
CYAN   := \033[0;36m
NC     := \033[0m

# ─── Targets ─────────────────────────────────────────────────────────────────

.PHONY: help
help: ## Show this help message
	@printf "\n${CYAN}${APP_NAME} — Makefile${NC}\n"
	@printf "${YELLOW}Usage:${NC} make ${GREEN}<target>${NC}\n\n"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| sort \
		| awk 'BEGIN {FS = ":.*?## "}; \
			{printf "${GREEN}%-28s${NC} %s\n", $$1, $$2}'
	@echo ""

# ─── Docker Compose ──────────────────────────────────────────────────────────

.PHONY: up
up: ## Start all services (production) using .env
	@echo "${BLUE}Starting all services...${NC}"
	docker compose --env-file .env up -d --build

.PHONY: up-dev
up-dev: ## Start services for development (with hot-reload)
	@echo "${BLUE}Starting development environment...${NC}"
	docker compose --env-file .env -f docker-compose.yml -f docker-compose.dev.yml up -d --build

.PHONY: down
down: ## Stop all services and remove containers
	@echo "${YELLOW}Stopping services...${NC}"
	docker compose down

.PHONY: down-volumes
down-volumes: ## Stop services and remove all data volumes
	@echo "${RED}WARNING: This will delete all data!${NC}"
	@read -p "Are you sure? [y/N] " -n 1 -r; echo; if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker compose down -v; \
		echo "${GREEN}Volumes removed.${NC}"; \
	fi

.PHONY: logs
logs: ## Tail logs of all services
	docker compose logs -f

.PHONY: logs-backend
logs-backend: ## Tail backend logs
	docker compose logs -f backend

.PHONY: logs-frontend
logs-frontend: ## Tail frontend logs
	docker compose logs -f frontend

.PHONY: ps
ps: ## List running containers
	docker compose ps

.PHONY: restart
restart: ## Restart all services
	docker compose restart

.PHONY: restart-backend
restart-backend: ## Restart backend only
	docker compose restart backend

# ─── Health Checks ───────────────────────────────────────────────────────────

.PHONY: health
health: ## Check health of all services
	@echo "${BLUE}Backend health:${NC}"
	-@curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || echo "unavailable"
	@echo "${BLUE}Frontend health:${NC}"
	-@curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/ || echo "unavailable"
	@echo ""
	@echo "${BLUE}Redis ping:${NC}"
	-@docker compose exec redis redis-cli ping 2>/dev/null || echo "unavailable"
	@echo "${BLUE}PostgreSQL ping:${NC}"
	-@docker compose exec postgres pg_isready 2>/dev/null || echo "unavailable"

.PHONY: wait
wait: ## Wait for all services to be healthy
	@echo "${BLUE}Waiting for services to be healthy...${NC}"
	@for i in $$(seq 1 30); do \
		status=0; \
		curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1 || status=1; \
		curl -sf -o /dev/null http://localhost:3000/ > /dev/null 2>&1 || status=1; \
		if [ $$status -eq 0 ]; then \
			echo "${GREEN}All services are healthy!${NC}"; \
			exit 0; \
		fi; \
		echo "Waiting... ($${i}/30)"; \
		sleep 3; \
	done; \
	echo "${RED}Timeout waiting for services${NC}"; \
	exit 1

# ─── Backend ─────────────────────────────────────────────────────────────────

.PHONY: backend-build
backend-build: ## Build backend JAR (without tests)
	@echo "${BLUE}Building backend...${NC}"
	cd $(BACKEND_DIR) && ./mvnw clean package -DskipTests -q

.PHONY: backend-build-skip
backend-build-skip: ## Quick backend build (skip all checks)
	cd $(BACKEND_DIR) && ./mvnw clean package -DskipTests -DskipITs -Dcheckstyle.skip -q

.PHONY: backend-test
backend-test: ## Run backend tests
	@echo "${BLUE}Running backend tests...${NC}"
	cd $(BACKEND_DIR) && ./mvnw test

.PHONY: backend-test-coverage
backend-test-coverage: ## Run backend tests with coverage report
	cd $(BACKEND_DIR) && ./mvnw verify jacoco:report
	@echo "${GREEN}Coverage report: backend/target/site/jacoco/index.html${NC}"

.PHONY: backend-lint
backend-lint: ## Run backend static analysis
	cd $(BACKEND_DIR) && ./mvnw checkstyle:check

.PHONY: backend-shell
backend-shell: ## Open a shell in the backend container
	docker compose exec backend sh

# ─── Frontend ────────────────────────────────────────────────────────────────

.PHONY: frontend-install
frontend-install: ## Install frontend dependencies
	@echo "${BLUE}Installing frontend dependencies...${NC}"
	cd $(FRONTEND_DIR) && npm ci --frozen-lockfile

.PHONY: frontend-dev
frontend-dev: ## Start frontend dev server
	cd $(FRONTEND_DIR) && npm run dev

.PHONY: frontend-build
frontend-build: ## Build frontend for production
	@echo "${BLUE}Building frontend...${NC}"
	cd $(FRONTEND_DIR) && npm run build

.PHONY: frontend-lint
frontend-lint: ## Lint frontend code
	cd $(FRONTEND_DIR) && npm run lint

.PHONY: frontend-typecheck
frontend-typecheck: ## Run TypeScript type checking
	cd $(FRONTEND_DIR) && npx tsc --noEmit

.PHONY: frontend-shell
frontend-shell: ## Open a shell in the frontend container
	docker compose exec frontend sh

# ─── Database ────────────────────────────────────────────────────────────────

.PHONY: db-migrate
db-migrate: ## Run Flyway database migrations manually
	docker compose exec backend sh -c "java -jar app.jar --spring.flyway.clean-disabled=false"

.PHONY: db-reset
db-reset: ## Reset database (destroys all data)
	@echo "${RED}WARNING: This will DESTROY all database data!${NC}"
	@read -p "Are you sure? [y/N] " -n 1 -r; echo; if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker compose down -v postgres; \
		docker compose up -d postgres; \
		echo "${GREEN}Database reset complete.${NC}"; \
	fi

.PHONY: db-shell
db-shell: ## Open PostgreSQL CLI
	docker compose exec postgres psql -U ${POSTGRES_USER:-fintrack} -d ${POSTGRES_DB:-fintrack}

.PHONY: db-backup
db-backup: ## Backup database to file
	@mkdir -p backups
	@docker compose exec -T postgres pg_dump -U ${POSTGRES_USER:-fintrack} ${POSTGRES_DB:-fintrack} \
		> backups/fintrack-$$(date +%Y%m%d_%H%M%S).sql
	@echo "${GREEN}Backup saved to backups/ directory${NC}"

.PHONY: db-restore
db-restore: ## Restore database from file (usage: make db-restore FILE=backup.sql)
	@if [ -z "$(FILE)" ]; then \
		echo "${RED}Usage: make db-restore FILE=backup.sql${NC}"; exit 1; fi
	@if [ ! -f "$(FILE)" ]; then \
		echo "${RED}File $(FILE) not found${NC}"; exit 1; fi
	@cat $(FILE) | docker compose exec -T postgres psql -U ${POSTGRES_USER:-fintrack} -d ${POSTGRES_DB:-fintrack}
	@echo "${GREEN}Database restored from $(FILE)${NC}"

# ─── Redis ───────────────────────────────────────────────────────────────────

.PHONY: redis-flush
redis-flush: ## Flush all Redis data
	@echo "${RED}WARNING: This will clear all Redis data!${NC}"
	@read -p "Are you sure? [y/N] " -n 1 -r; echo; if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker compose exec redis redis-cli FLUSHALL; \
		echo "${GREEN}Redis flushed.${NC}"; \
	fi

.PHONY: redis-shell
redis-shell: ## Open Redis CLI
	docker compose exec redis redis-cli

# ─── Ollama ──────────────────────────────────────────────────────────────────

.PHONY: ollama-pull
ollama-pull: ## Pull the chat model into Ollama
	docker compose exec -T ollama ollama pull ${OLLAMA_CHAT_MODEL:-dolphin-phi}
	@echo "${GREEN}Model pulled.${NC}"

.PHONY: ollama-list
ollama-list: ## List available Ollama models
	docker compose exec ollama ollama list

# ─── Secrets ──────────────────────────────────────────────────────────────────

.PHONY: secret-generate
secret-generate: ## Generate strong secrets for .env
	@echo "${BLUE}Generating secure random values...${NC}"
	@echo "JWT_SECRET=$$(openssl rand -base64 64 | tr -d '\n' | head -c 64)"
	@echo "POSTGRES_PASSWORD=$$(openssl rand -base64 24)"
	@echo "REDIS_PASSWORD=$$(openssl rand -base64 24)"

.PHONY: env-example
env-example: ## Create .env from example if not exists
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "${GREEN}.env created from .env.example${NC}"; \
		echo "${YELLOW}Edit .env with your secrets before running 'make up'${NC}"; \
	else \
		echo "${YELLOW}.env already exists, skipping${NC}"; \
	fi

# ─── SSL / TLS (for production with domain) ──────────────────────────────────

.PHONY: ssl-setup
ssl-setup: ## Generate self-signed SSL cert (dev only)
	@mkdir -p ssl
	@openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
		-keyout ssl/fintrack.key \
		-out ssl/fintrack.crt \
		-subj "/C=IN/ST=State/L=City/O=FinTrack AI/CN=localhost"
	@echo "${GREEN}Self-signed cert generated in ssl/ directory${NC}"

# ─── Maintenance ─────────────────────────────────────────────────────────────

.PHONY: clean
clean: ## Clean build artifacts
	@echo "${BLUE}Cleaning build artifacts...${NC}"
	cd $(BACKEND_DIR) && ./mvnw clean -q 2>/dev/null || true
	rm -rf $(FRONTEND_DIR)/dist $(FRONTEND_DIR)/.vite
	rm -rf backups/
	@echo "${GREEN}Clean complete.${NC}"

.PHONY: prune
prune: ## Remove all unused Docker resources
	docker system prune -af --volumes

.PHONY: info
info: ## Show project and Docker info
	@echo "${CYAN}════════════════════════════════════════${NC}"
	@echo "${CYAN}  ${APP_NAME} — Project Info${NC}"
	@echo "${CYAN}════════════════════════════════════════${NC}"
	@echo "${BLUE}Java:${NC}"
	cd $(BACKEND_DIR) && ./mvnw --version 2>/dev/null | grep "Java version" || echo "Not available"
	@echo "${BLUE}Node:${NC}"
	node --version 2>/dev/null || echo "Not available"
	@echo "${BLUE}Docker:${NC}"
	docker --version 2>/dev/null || echo "Not available"
	@echo "${BLUE}Docker Compose:${NC}"
	docker compose version 2>/dev/null || echo "Not available"
	@echo ""

# ─── CI/CD Helpers ───────────────────────────────────────────────────────────

.PHONY: ci-validate
ci-validate: ## Run all CI checks (lint, typecheck, test, build)
	@echo "${BLUE}Running CI validation...${NC}"
	$(MAKE) frontend-lint || exit 1
	$(MAKE) frontend-typecheck || exit 1
	$(MAKE) frontend-build || exit 1
	$(MAKE) backend-lint || exit 1
	$(MAKE) backend-test || exit 1
	$(MAKE) backend-build || exit 1
	@echo "${GREEN}All CI checks passed!${NC}"

.PHONY: deploy
deploy: ## Pull latest, rebuild, and restart services
	@echo "${BLUE}Deploying latest version...${NC}"
	git pull --ff-only
	docker compose --env-file .env up -d --build
	@echo "${GREEN}Deployment complete.${NC}"

# ─── Shell Linter ────────────────────────────────────────────────────────────

.PHONY: checkmake
checkmake: ## Validate this Makefile
	@echo "${BLUE}Validating Makefile...${NC}"
	@make -n -q help 2>/dev/null || true
	@echo "${GREEN}Makefile is valid.${NC}"

# ─── Development Shortcuts ───────────────────────────────────────────────────

.PHONY: dev
dev: ## Full development setup (install deps + start)
	@echo "${BLUE}Setting up development environment...${NC}"
	$(MAKE) env-example
	$(MAKE) frontend-install
	$(MAKE) up-dev
	$(MAKE) ollama-pull
	$(MAKE) wait
	@echo "${GREEN}Development environment ready!${NC}"
	@echo "  Frontend: ${CYAN}http://localhost:5173${NC}"
	@echo "  Backend:  ${CYAN}http://localhost:8080${NC}"
	@echo "  API Docs: ${CYAN}http://localhost:8080/api/v1/swagger/swagger-ui.html${NC}"

.PHONY: reset
reset: down-volumes clean ## Full reset (destroy data + clean artifacts)
	@echo "${GREEN}Full reset complete. Run 'make dev' to start fresh.${NC}"

# ─── Performance ─────────────────────────────────────────────────────────────

.PHONY: benchmark
benchmark: ## Run simple API benchmark
	@echo "${BLUE}Running API benchmarks...${NC}"
	@if command -v hey &> /dev/null; then \
		echo "Benchmarking /actuator/health..."; \
		hey -n 1000 -c 10 http://localhost:8080/actuator/health; \
	elif command -v ab &> /dev/null; then \
		echo "Benchmarking /actuator/health..."; \
		ab -n 1000 -c 10 http://localhost:8080/actuator/health; \
	else \
		echo "${YELLOW}Neither 'hey' nor 'ab' found. Install one for benchmarking.${NC}"; \
		echo "  apt install apache2-utils   # for ab"; \
		echo "  go install github.com/rakyll/hey@latest  # for hey"; \
	fi

print-%:  ## Print variable value (debugging): make print-VARIABLE
	@echo "$*=$($*)"
