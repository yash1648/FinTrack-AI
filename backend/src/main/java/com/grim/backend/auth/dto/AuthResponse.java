package com.grim.backend.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {
}
