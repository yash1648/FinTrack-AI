package com.grim.backend.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateBudgetRequest(
        @NotNull(message = "Limit amount is required")
        @Positive(message = "Limit amount must be positive")
        BigDecimal limitAmount
) {
}
