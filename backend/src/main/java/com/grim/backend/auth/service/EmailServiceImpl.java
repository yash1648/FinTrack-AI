package com.grim.backend.auth.service;

import com.grim.backend.common.exception.EmailDeliveryException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:NOT_FOUND}")
    private String fromEmail;





    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify your email - FinTrack AI";
        String content = "Hello,\n\nPlease verify your email using the following link: " +
                frontendUrl + "/verify-email?token=" + token +
                "\n\nThis link will expire in 15 minutes.";

        sendEmail(to, subject, content);
    }

    @Override
    public void sendLockoutNotification(String to, long minutes) {
        String subject = "Account Locked - FinTrack AI";
        String content = "Hello,\n\nYour account has been locked due to too many failed login attempts. " +
                "It will be automatically unlocked in " + minutes + " minutes.\n\n" +
                "If this wasn't you, please reset your password immediately.";

        sendEmail(to, subject, content);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Reset your password - FinTrack AI";
        String content = "Hello,\n\nPlease reset your password using the following link: " +
                frontendUrl + "/reset-password?token=" + token +
                "\n\nThis link will expire in 30 minutes.";

        sendEmail(to, subject, content);
    }

    private void sendEmail(String to, String subject, String content) {
        log.info("PREPARING EMAIL to: {} | subject: {} | content: {}", to, subject, content);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new EmailDeliveryException("Failed to send email to " + to, e);
        }
    }

    @Override
    public void sendBudgetAlert(String to, String category, String status,
                                 java.math.BigDecimal spent, java.math.BigDecimal limit) {
        String subject = "Budget Alert - FinTrack AI";
        String content = String.format(
            "Hello,\n\nYour budget for '%s' has been %s. Spent: %s / Limit: %s.",
            category, status, spent, limit);
        sendEmail(to, subject, content);
    }
}
