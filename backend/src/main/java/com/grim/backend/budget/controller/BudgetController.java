package com.grim.backend.budget.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.security.CustomUserDetails;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBudgets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Short month,
            @RequestParam(required = false) Short year) {
        return ResponseEntity.ok(new ApiResponse<>(true, budgetService.getBudgets(userDetails.getId(), month, year)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateBudgetRequest request) {
        Budget budget = budgetService.createBudget(userDetails.getId(), request.categoryId(), request.limitAmount(), request.month(), request.year());
        
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        Budget budget = budgetService.updateBudget(userDetails.getId(), id, request.limitAmount());
        
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        budgetService.deleteBudget(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
