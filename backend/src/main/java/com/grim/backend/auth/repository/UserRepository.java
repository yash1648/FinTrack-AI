package com.grim.backend.auth.repository;

import com.grim.backend.auth.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends
        JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u
        SET u.emailVerified = true,
            u.verificationToken = null
        WHERE u.id = :id
    """)
    void markEmailVerified(UUID id);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u
        SET u.passwordHash = :passwordHash
        WHERE u.id = :id
    """)
    void updatePassword(UUID id, String passwordHash);
}
