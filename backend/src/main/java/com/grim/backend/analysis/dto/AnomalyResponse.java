package com.grim.backend.analysis.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnomalyResponse {

    private UUID transactionId;
    private LocalDate date;
    private BigDecimal amount;
    private String category;

    private BigDecimal average;
    private String deviation;
    private String reason;
}