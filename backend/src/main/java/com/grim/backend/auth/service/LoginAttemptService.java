package com.grim.backend.auth.service;

import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private static final int MAX_ATTEMPTS = 5;
    private static final int INITIAL_LOCK_TIME_MINS = 15;
    private static final String LOCKOUT_KEY_PREFIX = "lockout:";

    public void loginSucceeded(String email) {
        redisTemplate.delete(LOCKOUT_KEY_PREFIX + email);
    }

    public void loginFailed(String email) {
        String key = LOCKOUT_KEY_PREFIX + email;
        Map<String, Object> lockoutData = (Map<String, Object>) redisTemplate.opsForValue().get(key);

        if (lockoutData == null) {
            lockoutData = new HashMap<>();
            lockoutData.put("attempts", 1);
            lockoutData.put("lockoutCount", 0);
        } else {
            int attempts = (int) lockoutData.get("attempts");
            lockoutData.put("attempts", attempts + 1);
        }

        int currentAttempts = (int) lockoutData.get("attempts");
        if (currentAttempts >= MAX_ATTEMPTS) {
            int lockoutCount = (int) lockoutData.get("lockoutCount") + 1;
            long lockTime = INITIAL_LOCK_TIME_MINS * (long) Math.pow(2, lockoutCount - 1);
            
            lockoutData.put("lockedUntil", LocalDateTime.now().plusMinutes(lockTime).toString());
            lockoutData.put("lockoutCount", lockoutCount);
            lockoutData.put("attempts", 0); // reset attempts after lockout

            log.warn("Account {} locked for {} minutes", email, lockTime);
            
            emailService.sendLockoutNotification(email, lockTime);

            userRepository.findByEmail(email).ifPresent(user -> {
                notificationService.sendSecurityAlert(user, "Account Locked", 
                    "Your account has been locked for " + lockTime + " minutes due to multiple failed login attempts.");
            });
        }

        redisTemplate.opsForValue().set(key, lockoutData, Duration.ofDays(1));
    }

    public boolean isLocked(String email) {
        String key = LOCKOUT_KEY_PREFIX + email;
        Map<String, Object> lockoutData = (Map<String, Object>) redisTemplate.opsForValue().get(key);

        if (lockoutData != null && lockoutData.containsKey("lockedUntil")) {
            LocalDateTime lockedUntil = LocalDateTime.parse((String) lockoutData.get("lockedUntil"));
            if (lockedUntil.isAfter(LocalDateTime.now())) {
                return true;
            }
        }
        return false;
    }

    public long getRemainingLockTime(String email) {
        String key = LOCKOUT_KEY_PREFIX + email;
        Map<String, Object> lockoutData = (Map<String, Object>) redisTemplate.opsForValue().get(key);

        if (lockoutData != null && lockoutData.containsKey("lockedUntil")) {
            LocalDateTime lockedUntil = LocalDateTime.parse((String) lockoutData.get("lockedUntil"));
            return Duration.between(LocalDateTime.now(), lockedUntil).toMinutes();
        }
        return 0;
    }
}
