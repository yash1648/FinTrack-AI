package com.grim.backend.nlp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParseResponse {
    private boolean parsed;

    private DraftTransactionDTO draft;

    private String source;
}
