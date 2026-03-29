package com.grim.backend.auth.controller;

import com.grim.backend.auth.dto.*;
import com.grim.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<com.grim.backend.auth.dto.ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
            ) {

        AuthResponse response=authService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        response
                ));

    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ){
        AuthResponse response = authService.loginUser(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, response)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody RefreshTokenRequest request
    ){
        AuthResponse response = authService.refreshToken(request.refreshToken());

        return ResponseEntity.ok(
                new ApiResponse<>(true, response)
        );
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @RequestBody RefreshTokenRequest request
    ){

        authService.logout(request.refreshToken());

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        new MessageResponse("Logged out successfully"))
        );
    }

}
