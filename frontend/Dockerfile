# =============================================================================
# Stage 1: Build the React SPA with Vite
# =============================================================================
FROM node:22-alpine AS build

WORKDIR /app

# Install dependencies (cached layer)
COPY package.json package-lock.json ./
RUN npm ci --frozen-lockfile --no-audit --no-fund

# Copy source and build
COPY . .

ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

# =============================================================================
# Stage 2: Serve with Nginx (alpine, minimal footprint)
# =============================================================================
FROM nginx:1.27-alpine

# Remove default nginx config and static assets
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built frontend assets
COPY --from=build /app/dist /usr/share/nginx/html

# Security: run as non-root user
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# Health check
RUN apk add --no-cache wget

EXPOSE 80

USER nginx

STOPSIGNAL SIGQUIT

CMD ["nginx", "-g", "daemon off;"]
