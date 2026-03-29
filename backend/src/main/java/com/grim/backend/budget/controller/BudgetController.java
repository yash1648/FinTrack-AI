package com.grim.backend.budget.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.security.JwtProvider;
import com.grim.backend.budget.dto.BudgetResponse;
import com.grim.backend.budget.dto.CreateBudgetRequest;
import com.grim.backend.budget.dto.UpdateBudgetRequest;
import com.grim.backend.budget.entity.Budget;
import com.grim.backend.budget.service.BudgetService;
import com.grim.backend.category.dto.CategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final JwtProvider jwtProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBudgets(@RequestHeader("Authorization") String token) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        return ResponseEntity.ok(new ApiResponse<>(true, budgetService.getBudgets(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateBudgetRequest request) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        Budget budget = budgetService.createBudget(userId, request.categoryId(), request.limitAmount(), request.month(), request.year());
        
        BudgetResponse response = new BudgetResponse(
                budget.getId(),
                new CategoryResponse(budget.getCategory().getId(), budget.getCategory().getName(), budget.getCategory().isDefault()),
                budget.getLimitAmount(),
                BigDecimal.ZERO,
                budget.getLimitAmount(),
                BigDecimal.ZERO,
                "ok",
                budget.getMonth(),
                budget.getYear()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        Budget budget = budgetService.updateBudget(userId, id, request.limitAmount());
        
        BudgetResponse response = new BudgetResponse(
                budget.getId(),
                new CategoryResponse(budget.getCategory().getId(), budget.getCategory().getName(), budget.getCategory().isDefault()),
                budget.getLimitAmount(),
                null, null, null, null,
                budget.getMonth(),
                budget.getYear()
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.noContent().build();
    }
}
