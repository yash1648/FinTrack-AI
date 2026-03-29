package com.grim.backend.category.repository;

import com.grim.backend.auth.entity.User;
import com.grim.backend.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId OR c.user IS NULL")
    List<Category> findAllForUser(@Param("userId") UUID userId);

    Optional<Category> findByNameAndUser(String name, User user);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.user.id = :userId OR c.user IS NULL)")
    Optional<Category> findByIdAndUser(@Param("id") UUID id, @Param("userId") UUID userId);

    Optional<Category> findByNameAndIsDefaultTrue(String name);
}
