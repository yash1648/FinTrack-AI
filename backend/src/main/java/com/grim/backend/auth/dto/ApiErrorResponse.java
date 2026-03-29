package com.grim.backend.auth.dto;

public record ApiErrorResponse(
        boolean success,
        ErrorDetail error
) {
    public static ApiErrorResponse of(int code, String message) {
        return new ApiErrorResponse(false, new ErrorDetail(code, message));
    }
}
