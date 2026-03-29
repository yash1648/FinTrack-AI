package com.grim.backend.transaction.dto;

import com.grim.backend.category.dto.CategoryResponse;
import com.grim.backend.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID userId,
        BigDecimal amount,
        TransactionType type,
        CategoryResponse category,
        String description,
        LocalDate date,
        Instant createdAt,
        Instant updatedAt
) {
}
