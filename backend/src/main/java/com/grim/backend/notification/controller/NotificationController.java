package com.grim.backend.notification.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.dto.MessageResponse;
import com.grim.backend.auth.dto.PaginationDto;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.notification.dto.NotificationResponse;
import com.grim.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unread_only,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        UUID userId = getUserId(authentication);
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, unread_only, pageable);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                notifications.getContent(),
                new PaginationDto(page, limit, (int) notifications.getTotalElements(), notifications.getTotalPages())
        ));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<MessageResponse>> markAsRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = getUserId(authentication);
        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Notification marked as read.")));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<MessageResponse>> markAllAsRead(
            Authentication authentication
    ) {
        UUID userId = getUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("All notifications marked as read.")));
    }

    private UUID getUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .getId();
    }
}
