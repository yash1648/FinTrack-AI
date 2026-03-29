package com.grim.backend.auth.service;

import com.grim.backend.auth.dto.AccessTokenResponse;
import com.grim.backend.auth.dto.AuthResponse;
import com.grim.backend.auth.dto.LoginRequest;
import com.grim.backend.auth.dto.RegisterRequest;
import com.grim.backend.auth.entity.RefreshToken;
import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.exception.AccountLockedException;
import com.grim.backend.auth.exception.ConflictException;
import com.grim.backend.auth.exception.EmailNotVerifiedException;
import com.grim.backend.auth.repository.RefreshTokenRepository;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.auth.security.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final LoginAttemptService loginAttemptService;
    private final PasswordBlocklistService passwordBlocklistService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse registerUser(RegisterRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new ConflictException("Email already exists");
        }

        if (passwordBlocklistService.isBlocked(request.password())) {
            throw new IllegalArgumentException("The password you provided is too common and insecure. Please choose a different one.");
        }

        String verificationToken=
                UUID.randomUUID().toString().replace("-", "");

        User user=User.builder()
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .verificationToken(verificationToken)
                .verificationTokenExpiry(LocalDateTime.now().plusMinutes(15))
                .emailVerified(false)
                .currency("INR")
                .active(true)
                .build();

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshTokenValue = generateRandomToken();
        String tokenHash = DigestUtils.sha256Hex(refreshTokenValue);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(token);

        return new AuthResponse(
                accessToken,
                refreshTokenValue
        );
    }


    @Transactional
    public AuthResponse loginUser(LoginRequest request){
        String email = request.email();

        if (loginAttemptService.isLocked(email)) {
            long remainingLockTime = loginAttemptService.getRemainingLockTime(email);
            throw new AccountLockedException("Account is locked due to multiple failed login attempts. Please try again after " + remainingLockTime + " minutes.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.password()
                    )
            );
            loginAttemptService.loginSucceeded(email);
        } catch (Exception e) {
            loginAttemptService.loginFailed(email);
            throw e;
        }

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->new UsernameNotFoundException("User not found with email: "+email));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email not verified. Please verify your email before logging in.");
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshTokenValue = generateRandomToken();
        String tokenHash = DigestUtils.sha256Hex(refreshTokenValue);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(token);

        return new AuthResponse(accessToken, refreshTokenValue);
    }


    @Transactional
    public AuthResponse refreshToken(String refreshToken){
        String tokenHash = DigestUtils.sha256Hex(refreshToken);
        RefreshToken token = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        if(token.getExpiresAt().isBefore(LocalDateTime.now())){
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = token.getUser();
        
        // Token Rotation: Delete old token and issue a new one
        refreshTokenRepository.delete(token);

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshTokenValue = generateRandomToken();
        String newTokenHash = DigestUtils.sha256Hex(newRefreshTokenValue);
        
        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(newToken);

        return new AuthResponse(
                newAccessToken,
                newRefreshTokenValue
        );
    }

    @Transactional
    public void logout(String refreshToken){
        String tokenHash = DigestUtils.sha256Hex(refreshToken);
        refreshTokenRepository.deleteByTokenHash(tokenHash);

        log.info("User logged out and refresh token revoked");

    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Hex.encodeHexString(bytes);
    }



}
