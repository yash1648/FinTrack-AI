package com.grim.backend.auth.dto;

public record ErrorDetail(
        int code,
        String message
) {
}
