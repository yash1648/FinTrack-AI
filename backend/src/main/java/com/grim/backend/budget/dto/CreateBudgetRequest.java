package com.grim.backend.budget.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateBudgetRequest(
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotNull(message = "Limit amount is required")
        @Positive(message = "Limit amount must be positive")
        BigDecimal limitAmount,

        @Min(1) @Max(12)
        short month,

        @Min(2000) @Max(2100)
        short year
) {
}
