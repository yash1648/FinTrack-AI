package com.grim.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        PaginationDto pagination
) {
    public ApiResponse(boolean success, T data) {
        this(success, data, null);
    }
}
