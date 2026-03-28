package com.grim.backend.auth.repository;

import com.grim.backend.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordResetTokenRepository extends
        JpaRepository<PasswordResetToken, UUID> {
}
