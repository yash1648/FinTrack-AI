package com.grim.backend.transaction.dto;

import com.grim.backend.transaction.entity.TransactionType;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTransactionRequest(
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        TransactionType type,

        UUID categoryId,

        @Size(max = 255, message = "Description must be less than 255 characters")
        String description,

        @PastOrPresent(message = "Date cannot be in the future")
        LocalDate date
) {
}
