package com.grim.backend.analysis.service;

import com.grim.backend.analysis.dto.ProjectionResponse;
import com.grim.backend.transaction.entity.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ProjectionService {

    public ProjectionResponse project(List<Transaction> txs) {

        BigDecimal spent = txs.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();

        int daysElapsed = today.getDayOfMonth();
        int daysInMonth = today.lengthOfMonth();

        BigDecimal projected = spent
                .divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(daysInMonth));

        return ProjectionResponse.builder()
                .spent(spent)
                .projected(projected)
                .daysElapsed(daysElapsed)
                .daysInMonth(daysInMonth)
                .currency("INR")
                .build();
    }
}