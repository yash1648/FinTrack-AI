package com.grim.backend.auth.dto;

public record PaginationDto(
        int page,
        int limit,
        long total,
        int totalPages
) {
}
