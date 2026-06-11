package com.grim.backend.auth.dto;

import java.util.List;

public record ApiErrorResponse(
        boolean success,
        ErrorDetail error
) {
    public static ApiErrorResponse of(int code, String message) {
        return new ApiErrorResponse(false, new ErrorDetail(code, message));
    }

    public static ApiErrorResponse of(int code, String message, List<FieldErrorDetail> fields) {
        return new ApiErrorResponse(false, new ErrorDetail(code, message, fields));
    }
}
