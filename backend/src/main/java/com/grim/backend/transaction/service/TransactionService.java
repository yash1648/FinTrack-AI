package com.grim.backend.transaction.service;

import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.budget.service.BudgetService;
import com.grim.backend.category.entity.Category;
import com.grim.backend.category.repository.CategoryRepository;
import com.grim.backend.common.exception.ResourceNotFoundException;
import com.grim.backend.transaction.entity.Transaction;
import com.grim.backend.transaction.entity.TransactionType;
import com.grim.backend.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;

    @Transactional
    public Transaction createTransaction(UUID userId, BigDecimal amount, TransactionType type, UUID categoryId, String description, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Category category = categoryRepository.findByIdAndUser(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible"));

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .category(category)
                .description(description)
                .date(date)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        budgetService.checkBudgetAfterTransaction(userId, categoryId, date);
        return savedTransaction;
    }

    public Page<Transaction> getTransactions(UUID userId, LocalDate from, LocalDate to, TransactionType type, UUID categoryId, BigDecimal minAmount, BigDecimal maxAmount, String search, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("date").descending());
        return transactionRepository.findFiltered(userId, from, to, type, categoryId, minAmount, maxAmount, search, pageable);
    }

    public Transaction getTransactionById(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found or not accessible"));
    }

    @Transactional
    public Transaction updateTransaction(UUID userId, UUID transactionId, BigDecimal amount, TransactionType type, UUID categoryId, String description, LocalDate date) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found or not accessible"));

        if (categoryId != null) {
            Category category = categoryRepository.findByIdAndUser(categoryId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible"));
            transaction.setCategory(category);
        }

        if (amount != null) transaction.setAmount(amount);
        if (type != null) transaction.setType(type);
        if (description != null) transaction.setDescription(description);
        if (date != null) transaction.setDate(date);

        Transaction updatedTransaction = transactionRepository.save(transaction);
        budgetService.checkBudgetAfterTransaction(userId, transaction.getCategory().getId(), transaction.getDate());
        return updatedTransaction;
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found or not accessible"));
        transactionRepository.delete(transaction);
    }

    public Map<String, Object> getDashboardSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

        List<Object[]> sums = transactionRepository.sumByType(userId, startOfMonth, endOfMonth);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Object[] result : sums) {
            TransactionType type = (TransactionType) result[0];
            BigDecimal sum = (BigDecimal) result[1];
            if (type == TransactionType.INCOME) totalIncome = sum;
            else if (type == TransactionType.EXPENSE) totalExpenses = sum;
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);
        List<Transaction> recentTransactions = transactionRepository.findTop5ByUserIdOrderByDateDesc(userId);
        List<Map<String, Object>> activeBudgets = budgetService.getBudgets(userId);
        
        List<Map<String, Object>> alerts = activeBudgets.stream()
                .filter(b -> !"ok".equals(b.get("status")))
                .map(b -> Map.of(
                        "budgetId", b.get("id"),
                        "categoryName", ((Map)b.get("category")).get("name"),
                        "status", b.get("status"),
                        "percentage", b.get("percentage")
                ))
                .toList();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpenses", totalExpenses);
        summary.put("netBalance", netBalance);
        summary.put("currency", user.getCurrency());
        summary.put("month", now.getMonth().name() + " " + now.getYear());
        summary.put("recentTransactions", recentTransactions.stream()
                .map(t -> Map.of(
                        "id", t.getId(),
                        "amount", t.getAmount(),
                        "type", t.getType(),
                        "category", Map.of("id", t.getCategory().getId(), "name", t.getCategory().getName()),
                        "description", t.getDescription(),
                        "date", t.getDate()
                ))
                .toList());
        summary.put("activeBudgetAlerts", alerts);

        return summary;
    }
}
