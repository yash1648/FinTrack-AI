package com.grim.backend.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtProvider {

    private final Key signingKey;
    private final long accessTokenexpirationTime;
    private final long refreshTokenexpirationTime;

    public JwtProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessExp,
            @Value("${jwt.refresh-expiration}") long refreshExp
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenexpirationTime = accessExp;
        this.refreshTokenexpirationTime = refreshExp;
    }

    public String generateAccessToken(UUID userId, String email) {

        log.info("Generating token for user {}", email);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId.toString())
                .claim("type","access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenexpirationTime))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type","refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenexpirationTime))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return parseToken(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String userIdStr = parseToken(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, String email) {
        return email.equals(extractEmail(token)) && !isTokenExpired(token);
    }
}