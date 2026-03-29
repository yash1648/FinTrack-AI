package com.grim.backend.transaction.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.security.JwtProvider;
import com.grim.backend.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;
    private final JwtProvider jwtProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary(@RequestHeader("Authorization") String token) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        Map<String, Object> summary = transactionService.getDashboardSummary(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, summary));
    }
}
