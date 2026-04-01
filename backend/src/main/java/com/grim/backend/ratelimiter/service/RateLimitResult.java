package com.grim.backend.ratelimiter.service;

public record RateLimitResult(
        boolean allowed,
        long retryAfterSeconds
) {
}

