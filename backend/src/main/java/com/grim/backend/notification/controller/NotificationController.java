package com.grim.backend.notification.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.dto.MessageResponse;
import com.grim.backend.auth.dto.PaginationDto;
import com.grim.backend.auth.security.CustomUserDetails;
import com.grim.backend.notification.dto.NotificationResponse;
import com.grim.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unread_only,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<NotificationResponse> notifications = notificationService.getNotifications(userDetails.getId(), unread_only, pageable);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                notifications.getContent(),
                new PaginationDto(page, limit, (int) notifications.getTotalElements(), notifications.getTotalPages())
        ));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<MessageResponse>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAsRead(userDetails.getId(), id);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Notification marked as read.")));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<MessageResponse>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("All notifications marked as read.")));
    }
}
