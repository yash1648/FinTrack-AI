package com.grim.backend.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightData {

    private boolean sufficient;

    private Instant cachedAt;

    private List<String> patterns;

    private List<AnomalyResponse> anomalies;

    private List<String> recommendations;

    private BigDecimal projectedMonthlyExpense;

    private String message;

}