package com.grim.backend.auth.controller;

import com.grim.backend.auth.dto.*;
import com.grim.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<com.grim.backend.auth.dto.ApiResponse<MessageResponse>> register(
            @Valid @RequestBody RegisterRequest request
                ) {



        authService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        new MessageResponse("Registration successful. Please verify your email using the link sent to your email.")
                ));

    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<MessageResponse>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Email verified successfully.")));
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
            @Valid @RequestBody RefreshTokenRequest request
    ){

        authService.logout(request.refreshToken());

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        new MessageResponse("Logged out successfully"))
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("If your email is registered, a reset link has been sent.")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Password reset successfully.")));
    }

    @GetMapping("/profile")

    public ResponseEntity<ApiResponse<UserDto>> getProfile(Principal principal) {
        UserDto profile = authService.getProfile(principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, profile));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserDto updated = authService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, updated));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Password changed successfully.")));
    }

}
