package com.grim.backend.ratelimiter.policy;

import java.time.Duration;
import java.util.Map;

/**
 * Central rate-limiting contract (must match docs/API.MD section 15).
 */
public final class RateLimiterPolicies {

    public static final String API_PREFIX = "/api/v1";

    private static final RateLimitRule AUTH_LOGIN = new RateLimitRule(
            10,
            Duration.ofMinutes(10),
            RateLimitScope.IP
    );

    private static final RateLimitRule AUTH_REGISTER = new RateLimitRule(
            5,
            Duration.ofHours(1),
            RateLimitScope.IP
    );

    private static final RateLimitRule AUTH_FORGOT_PASSWORD = new RateLimitRule(
            5,
            Duration.ofHours(1),
            RateLimitScope.IP
    );

    private static final RateLimitRule NLP_PARSE = new RateLimitRule(
            30,
            Duration.ofHours(1),
            RateLimitScope.USER
    );

    private static final RateLimitRule ANALYSIS_INSIGHTS = new RateLimitRule(
            20,
            Duration.ofHours(1),
            RateLimitScope.USER
    );

    public static final RateLimitRule DEFAULT_PROTECTED = new RateLimitRule(
            300,
            Duration.ofMinutes(15),
            RateLimitScope.USER
    );

    private static final Map<String, RateLimitRule> RULES = Map.of(
            key("POST", API_PREFIX + "/auth/login"), AUTH_LOGIN,
            key("POST", API_PREFIX + "/auth/register"), AUTH_REGISTER,
            key("POST", API_PREFIX + "/auth/forgot-password"), AUTH_FORGOT_PASSWORD,
            key("POST", API_PREFIX + "/nlp/parse"), NLP_PARSE,
            key("GET", API_PREFIX + "/analysis/insights"), ANALYSIS_INSIGHTS
    );

    private RateLimiterPolicies() {}

    public static RateLimitRule lookup(String httpMethod, String absolutePath) {
        return RULES.get(key(httpMethod, absolutePath));
    }

    private static String key(String method, String path) {
        return method + " " + path;
    }
}

