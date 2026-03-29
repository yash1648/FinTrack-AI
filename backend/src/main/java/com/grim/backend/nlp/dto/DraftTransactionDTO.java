package com.grim.backend.nlp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftTransactionDTO {

    private BigDecimal amount;

    private String type;  // expense | income | transfer

    private String categoryId;

    private String categoryName;

    private String description;

    private LocalDate date;
}