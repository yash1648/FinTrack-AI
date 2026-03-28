package com.grim.backend.auth.dto;

public record ApiResponse<T> (
        boolean success,
        T data
)
{
}
