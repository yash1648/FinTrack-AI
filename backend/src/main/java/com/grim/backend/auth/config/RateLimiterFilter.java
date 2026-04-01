package com.grim.backend.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grim.backend.auth.dto.ApiErrorResponse;
import com.grim.backend.auth.security.JwtProvider;
import com.grim.backend.ratelimiter.service.RateLimiterService;
import com.grim.backend.ratelimiter.service.RateLimitResult;
import com.grim.backend.ratelimiter.policy.RateLimitRule;
import com.grim.backend.ratelimiter.policy.RateLimiterPolicies;
import com.grim.backend.ratelimiter.policy.RateLimitScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiterService limiter;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;



    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String absolutePath = request.getRequestURI();

        // Allow tooling / docs endpoints through without limiting.
        if (absolutePath.startsWith("/api/v1/swagger")
                || absolutePath.contains("h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = RateLimiterPolicies.lookup(request.getMethod(), absolutePath);
        boolean hasAuthorization = request.getHeader("Authorization") != null;

        if (rule == null) {
            // Default to "all other protected endpoints" only when user is authenticated.
            if (!hasAuthorization) {
                filterChain.doFilter(request, response);
                return;
            }
            rule = RateLimiterPolicies.DEFAULT_PROTECTED;
        }

        String scopeKey = buildScopeKey(request, rule);
        RateLimitResult result;
        try {
            result = limiter.tryConsume(
                    scopeKey,
                    rule.limitRequests(),
                    rule.window()
            );
        } catch (Exception e) {
            // If rate limiter is unavailable (e.g., Redis down), fail open to keep API
            // operational. Auth endpoints are still safe because they have login attempt
            // tracking backed by Redis independently (LoginAttemptService).
            filterChain.doFilter(request, response);
            return;
        }

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiErrorResponse body = ApiErrorResponse.of(429, "Too many requests");
            PrintWriter writer = response.getWriter();
            objectMapper.writeValue(writer, body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String buildScopeKey(HttpServletRequest request, RateLimitRule rule) {
        String absolutePath = request.getRequestURI();
        String method = request.getMethod();

        if (rule.scope() == RateLimitScope.IP) {
            String ip = getClientIp(request);
            return "ip:" + ip + ":" + method + ":" + absolutePath;
        }

        UUID userId = extractUserId(request).orElse(null);
        if (userId == null) {
            // Fail-safe: if JWT is missing/unparseable, fall back to IP.
            String ip = getClientIp(request);
            return "ip:" + ip + ":" + method + ":" + absolutePath;
        }

        return "user:" + userId + ":" + method + ":" + absolutePath;
    }

    private Optional<UUID> extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        try {
            return Optional.of(jwtProvider.extractUserId(authHeader.substring(7)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // If there are multiple proxies, the first one is the original client.
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
