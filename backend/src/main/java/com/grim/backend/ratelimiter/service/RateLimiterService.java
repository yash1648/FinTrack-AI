package com.grim.backend.ratelimiter.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimiterService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RateLimitResult tryConsume(String key, int limit, Duration window) {
        String redisKey = "rate_limit:" + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            // Fail closed: don't accidentally remove protection.
            return new RateLimitResult(false, window.getSeconds());
        }

        if (count == 1) {
            redisTemplate.expire(redisKey, window);
        }

        boolean allowed = count <= limit;
        if (allowed) {
            return new RateLimitResult(true, 0);
        }

        Long ttlSeconds = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        long retryAfterSeconds = (ttlSeconds != null && ttlSeconds > 0)
                ? ttlSeconds
                : window.getSeconds();

        return new RateLimitResult(false, retryAfterSeconds);
    }
}