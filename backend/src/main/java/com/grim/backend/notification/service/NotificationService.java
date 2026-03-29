package com.grim.backend.notification.service;

import com.grim.backend.auth.entity.User;
import com.grim.backend.notification.dto.NotificationResponse;
import com.grim.backend.notification.entity.Notification;
import com.grim.backend.notification.repository.NotificationRepository;
import com.grim.backend.common.exception.ResourceNotFoundException;
import com.grim.backend.common.exception.ForbiddenActionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Page<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByUserIdAndIsReadFalse(userId, pageable);
        } else {
            notifications = notificationRepository.findByUserId(userId, pageable);
        }
        return notifications.map(this::mapToResponse);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenActionException("Notification not accessible");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    @Transactional
    public void sendBudgetAlert(User user, String categoryName, String status, BigDecimal spent, BigDecimal limit) {
        String title = "Budget " + (status.equals("exceeded") ? "Exceeded" : "Warning") + ": " + categoryName;
        String body = String.format("You have used %.2f%% of your %s budget. Spent: %.2f, Limit: %.2f",
                spent.divide(limit, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)),
                categoryName, spent, limit);

        Notification notification = Notification.builder()
                .user(user)
                .type("budget_" + status)
                .title(title)
                .body(body)
                .build();

        notificationRepository.save(notification);
        pushNotification(user.getId(), mapToResponse(notification));
    }

    @Transactional
    public void sendSecurityAlert(User user, String title, String body) {
        Notification notification = Notification.builder()
                .user(user)
                .type("security_alert")
                .title(title)
                .body(body)
                .build();

        notificationRepository.save(notification);
        pushNotification(user.getId(), mapToResponse(notification));
    }

    private void pushNotification(UUID userId, NotificationResponse payload) {
        String destination = "/topic/notifications/" + userId;
        messagingTemplate.convertAndSend(destination, payload);
        log.info("Pushed notification to user {}: {}", userId, payload.title());
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
