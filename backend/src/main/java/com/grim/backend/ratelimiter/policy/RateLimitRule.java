package com.grim.backend.ratelimiter.policy;

import java.time.Duration;

public record RateLimitRule(
        int limitRequests,
        Duration window,
        RateLimitScope scope
) {
}

