package com.grim.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(
        int code,
        String message,
        List<FieldErrorDetail> fields
) {
    public ErrorDetail(int code, String message) {
        this(code, message, null);
    }
}
