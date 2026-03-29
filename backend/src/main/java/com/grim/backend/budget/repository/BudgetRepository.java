package com.grim.backend.budget.repository;

import com.grim.backend.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.month = :month AND b.year = :year")
    List<Budget> findByUserAndPeriod(@Param("userId") UUID userId, @Param("month") short month, @Param("year") short year);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category.id = :categoryId AND b.month = :month AND b.year = :year")
    Optional<Budget> findByUserCategoryAndPeriod(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, @Param("month") short month, @Param("year") short year);

    boolean existsByUserIdAndCategoryIdAndMonthAndYear(UUID userId, UUID categoryId, short month, short year);
}
