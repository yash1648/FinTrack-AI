package com.grim.backend.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpirationTime;
    private final long refreshTokenExpirationTime;

    public JwtProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessExp,
            @Value("${jwt.refresh-expiration}") long refreshExp
    ) {
        // For HMAC-SHA256, the key must be at least 256 bits (32 bytes)
        byte[] keyBytes = secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("JWT secret is too short (< 32 bytes). Consider using a longer secret for production.");
        }
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.accessTokenExpirationTime = accessExp;
        this.refreshTokenExpirationTime = refreshExp;
    }

    public String generateAccessToken(UUID userId, String email) {
        log.info("Generating access token for user {}", email);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationTime))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationTime))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(60) // Allow 1 minute clock skew
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (JwtException e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    public UUID extractUserId(String token) {
        try {
            String userIdStr = parseToken(token).get("userId", String.class);
            return UUID.fromString(userIdStr);
        } catch (JwtException e) {
            log.warn("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public boolean isTokenValid(String token, String email) {
        try {
            Claims claims = parseToken(token);
            return email.equals(claims.getSubject()) && !claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }
}