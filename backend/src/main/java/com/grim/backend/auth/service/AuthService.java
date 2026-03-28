package com.grim.backend.auth.service;

import com.grim.backend.auth.dto.AccessTokenResponse;
import com.grim.backend.auth.dto.AuthResponse;
import com.grim.backend.auth.dto.LoginRequest;
import com.grim.backend.auth.dto.RegisterRequest;
import com.grim.backend.auth.entity.RefreshToken;
import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.exception.ConflictException;
import com.grim.backend.auth.repository.RefreshTokenRepository;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.auth.security.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Transactional
    public AuthResponse registerUser(RegisterRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new ConflictException("Email already exists");
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
                .currency("$")
                .active(true)
                .build();

        userRepository.save(user);

        //todo: Email service send request for the email verfication

        String accessToken = jwtProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());
        String tokenHash = DigestUtils.sha256Hex(refreshToken);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(token);

        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }


    @Transactional
    public AuthResponse loginUser(LoginRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user=userRepository.findByEmail(request.email())
                .orElseThrow(()->new UsernameNotFoundException("User not found with email: "+request.email()));

        String accessToken = jwtProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());
        String tokenHash = DigestUtils.sha256Hex(refreshToken);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(token);

        return new AuthResponse(accessToken, refreshToken);
    }


    @Transactional
    public AccessTokenResponse refreshToken(String refreshToken){
        String tokenHash = DigestUtils.sha256Hex(refreshToken);
        RefreshToken token = refreshTokenRepository
                .findByTokenHash(refreshToken)
                .orElseThrow(() ->
                        new RuntimeException("Invalid refresh token"));

        if(token.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new RuntimeException("Refresh token expired");
        }

        User user = token.getUser();

        String newAccessToken = jwtProvider.generateAccessToken(user.getEmail());

        return new AccessTokenResponse(
                newAccessToken
        );
    }

    @Transactional
    public void logout(String refreshToken){
        String tokenHash = DigestUtils.sha256Hex(refreshToken);
        refreshTokenRepository.deleteByTokenHash(tokenHash);

        log.info("User logged out and refresh token revoked");

    }



}
