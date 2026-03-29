package com.grim.backend.transaction.dto;

import com.grim.backend.transaction.entity.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionQuery (
        @Min(1)
        int page,
        @Min(1)
        @Max(100)
        int limit,
        LocalDate from,
        LocalDate to,
        TransactionType type,
        UUID categoryId,
        @Size(max = 100)
        String search,

        BigDecimal minAmount,
        BigDecimal maxAmount

){
    
}
