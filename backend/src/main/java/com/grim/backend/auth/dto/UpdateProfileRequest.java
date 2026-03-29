package com.grim.backend.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100)
        String name,
        @Size(min = 3, max = 3)
        String currency
) {
}
