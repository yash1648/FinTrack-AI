package com.grim.backend.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String body,
        boolean isRead,
        LocalDateTime createdAt
) {}
