package com.grim.backend.analysis.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectionResponse {

    private BigDecimal spent;
    private BigDecimal projected;
    private Integer daysElapsed;
    private Integer daysInMonth;
    private String currency;
}