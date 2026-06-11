package com.grim.backend.auth.dto;

public record FieldErrorDetail(
        String field,
        String message
) {}
