package com.grim.backend.transaction.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.security.CustomUserDetails;
import com.grim.backend.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> summary = transactionService.getDashboardSummary(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, summary));
    }
}
