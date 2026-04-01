package com.grim.backend.transaction.repository;

import com.grim.backend.transaction.entity.Transaction;
import com.grim.backend.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
            "AND (:from IS NULL OR t.date >= :from) " +
            "AND (:to IS NULL OR t.date <= :to) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
            "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
            "AND (:search IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Transaction> findFiltered(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") TransactionType type,
            @Param("categoryId") UUID categoryId,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.date BETWEEN :from AND :to " +
            "GROUP BY t.type")
    List<Object[]> sumByType(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Transaction> findTop5ByUserIdOrderByDateDesc(UUID userId);

    @Modifying
    @Query("UPDATE Transaction t SET t.category.id = :newCategoryId WHERE t.user.id = :userId AND t.category.id = :oldCategoryId")
    void reassignCategory(@Param("userId") UUID userId, @Param("oldCategoryId") UUID oldCategoryId, @Param("newCategoryId") UUID newCategoryId);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.category.id = :categoryId " +
            "AND t.type = 'EXPENSE' " +
            "AND MONTH(t.date) = :month AND YEAR(t.date) = :year")
    BigDecimal sumByCategoryAndPeriod(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.type = 'EXPENSE' " +
            "AND (:from IS NULL OR t.date >= :from) " +
            "AND (:to IS NULL OR t.date <= :to) " +
            "GROUP BY t.category.name")
    List<Object[]> sumByCategory(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT FUNCTION('YEAR', t.date) as year, FUNCTION('MONTH', t.date) as month, t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND (:from IS NULL OR t.date >= :from) " +
            "AND (:to IS NULL OR t.date <= :to) " +
            "GROUP BY FUNCTION('YEAR', t.date), FUNCTION('MONTH', t.date), t.type " +
            "ORDER BY year DESC, month DESC")
    List<Object[]> sumByMonth(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT t.date, t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND (:from IS NULL OR t.date >= :from) " +
            "AND (:to IS NULL OR t.date <= :to) " +
            "GROUP BY t.date, t.type " +
            "ORDER BY t.date ASC")
    List<Object[]> sumByDay(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
SELECT t FROM Transaction t
WHERE t.user.id = :userId
AND t.type = 'EXPENSE'
AND t.date >= :startDate
""")
    List<Transaction> findLast90Days(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate);
}
