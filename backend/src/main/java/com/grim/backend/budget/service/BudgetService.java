package com.grim.backend.budget.service;

import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.budget.entity.Budget;
import com.grim.backend.budget.repository.BudgetRepository;
import com.grim.backend.category.entity.Category;
import com.grim.backend.category.repository.CategoryRepository;
import com.grim.backend.common.exception.ConflictException;
import com.grim.backend.common.exception.ForbiddenActionException;
import com.grim.backend.common.exception.ResourceNotFoundException;
import com.grim.backend.transaction.repository.TransactionRepository;
import com.grim.backend.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Transactional
    public Budget createBudget(UUID userId, UUID categoryId, BigDecimal limitAmount, short month, short year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        categoryRepository.findByIdAndUser(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible"));

        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, month, year)) {
            throw new ConflictException("Budget already exists for this period and category");
        }

        Budget budget = Budget.builder()
                .user(user)
                .category(categoryRepository.findById(categoryId).get())
                .limitAmount(limitAmount)
                .month(month)
                .year(year)
                .build();

        return budgetRepository.save(budget);
    }

    public List<Map<String, Object>> getBudgets(UUID userId) {
        LocalDate now = LocalDate.now();
        short month = (short) now.getMonthValue();
        short year = (short) now.getYear();

        List<Budget> budgets = budgetRepository.findByUserAndPeriod(userId, month, year);
        List<Map<String, Object>> enrichedBudgets = new ArrayList<>();

        for (Budget budget : budgets) {
            BigDecimal spent = transactionRepository.sumByCategoryAndPeriod(userId, budget.getCategory().getId(), month, year);
            if (spent == null) spent = BigDecimal.ZERO;

            BigDecimal percentage = spent.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
            String status = "ok";
            if (percentage.compareTo(new BigDecimal(100)) >= 0) status = "exceeded";
            else if (percentage.compareTo(new BigDecimal(80)) >= 0) status = "warning";

            enrichedBudgets.add(Map.of(
                    "id", budget.getId(),
                    "category", Map.of("id", budget.getCategory().getId(), "name", budget.getCategory().getName()),
                    "limitAmount", budget.getLimitAmount(),
                    "spent", spent,
                    "remaining", budget.getLimitAmount().subtract(spent),
                    "percentage", percentage,
                    "status", status,
                    "month", budget.getMonth(),
                    "year", budget.getYear()
            ));
        }

        return enrichedBudgets;
    }

    @Transactional
    public Budget updateBudget(UUID userId, UUID budgetId, BigDecimal limitAmount) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new ForbiddenActionException("Budget not accessible");
        }

        budget.setLimitAmount(limitAmount);
        return budgetRepository.save(budget);
    }

    @Transactional
    public void deleteBudget(UUID userId, UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new ForbiddenActionException("Budget not accessible");
        }

        budgetRepository.delete(budget);
    }

    @Transactional
    public void checkBudgetAfterTransaction(UUID userId, UUID categoryId, LocalDate date) {
        short month = (short) date.getMonthValue();
        short year = (short) date.getYear();

        budgetRepository.findByUserCategoryAndPeriod(userId, categoryId, month, year)
                .ifPresent(budget -> {
                    BigDecimal spent = transactionRepository.sumByCategoryAndPeriod(userId, categoryId, month, year);
                    if (spent == null) spent = BigDecimal.ZERO;

                    BigDecimal limit = budget.getLimitAmount();
                    BigDecimal percentage = spent.divide(limit, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));

                    if (percentage.compareTo(new BigDecimal(100)) >= 0) {
                        log.warn("BUDGET EXCEEDED: User {} category {} spent {} limit {}", userId, budget.getCategory().getName(), spent, limit);
                        notificationService.sendBudgetAlert(budget.getUser(), budget.getCategory().getName(), "exceeded", spent, limit);
                    } else if (percentage.compareTo(new BigDecimal(80)) >= 0) {
                        log.warn("BUDGET WARNING: User {} category {} spent {} limit {}", userId, budget.getCategory().getName(), spent, limit);
                        notificationService.sendBudgetAlert(budget.getUser(), budget.getCategory().getName(), "warning", spent, limit);
                    }
                });
    }
}
