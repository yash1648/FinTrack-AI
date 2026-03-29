package com.grim.backend.auth.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendLockoutNotification(String to, long minutes);
}
