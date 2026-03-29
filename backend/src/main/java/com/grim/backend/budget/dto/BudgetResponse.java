package com.grim.backend.budget.dto;

import com.grim.backend.category.dto.CategoryResponse;
import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        CategoryResponse category,
        BigDecimal limitAmount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentage,
        String status,
        short month,
        short year
) {
}
