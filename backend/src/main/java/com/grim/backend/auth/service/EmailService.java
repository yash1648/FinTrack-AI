package com.grim.backend.auth.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendLockoutNotification(String to, long minutes);
    void sendPasswordResetEmail(String to, String token);
    void sendBudgetAlert(String to, String category, String status, java.math.BigDecimal spent, java.math.BigDecimal limit);
}
