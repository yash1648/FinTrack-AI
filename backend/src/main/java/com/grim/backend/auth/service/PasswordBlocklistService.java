package com.grim.backend.auth.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PasswordBlocklistService {
    // In a real app, this would be loaded from a file or database
    private static final Set<String> BLOCKLIST = Set.of(
            "password", "12345678", "qwertyuiop", "password123", "admin123"
            // ... imagine 10,000 more here
    );

    public boolean isBlocked(String password) {
        return BLOCKLIST.contains(password.toLowerCase());
    }
}
