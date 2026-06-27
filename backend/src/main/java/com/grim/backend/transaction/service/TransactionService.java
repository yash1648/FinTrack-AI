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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        transactionRepository.flush(); // Ensure the transaction is visible for budget check
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
        transactionRepository.flush(); // Ensure the transaction is visible for budget check
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
        LocalDate weekAgo = now.minusDays(7);

        // Execute all independent DB queries in parallel-ish using a single orchestrating approach:
        // 1. Monthly income/expense sums
        List<Object[]> sums = transactionRepository.sumByType(userId, startOfMonth, endOfMonth);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Object[] result : sums) {
            TransactionType type = (TransactionType) result[0];
            BigDecimal val = (BigDecimal) result[1];
            if (type == TransactionType.INCOME) totalIncome = val;
            else if (type == TransactionType.EXPENSE) totalExpenses = val;
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        // 2. Recent transactions + spending trend + budgets (all from DB)
        List<Transaction> recentTransactions = transactionRepository.findTop5ByUserIdOrderByDateDesc(userId);
        List<Object[]> trend = transactionRepository.sumByDay(userId, weekAgo, now);
        List<Map<String, Object>> activeBudgets = budgetService.getBudgets(userId, null, null);

        // Category breakdown for expense pie chart
        List<Object[]> breakdown = transactionRepository.sumByCategory(userId, startOfMonth, endOfMonth);

        // Build alerts from budgets
        List<Map<String, Object>> alerts = activeBudgets.stream()
                .filter(b -> !"ok".equals(b.get("status")))
                .map(b -> {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("budgetId", b.get("id"));
                    alert.put("categoryName", ((Map) b.get("category")).get("name"));
                    alert.put("status", b.get("status"));
                    alert.put("percentage", b.get("percentage"));
                    alert.put("spent", b.get("spent"));
                    alert.put("limit", b.get("limitAmount"));
                    return alert;
                })
                .toList();

        // Build recent spending for last 7 days
        List<Map<String, Object>> recentSpending = trend.stream()
                .filter(r -> r[1] == TransactionType.EXPENSE)
                .map(r -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date", r[0]);
                    entry.put("amount", r[2]);
                    return entry;
                })
                .toList();

        // Assemble summary (camelCase keys to match frontend expectations)
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpenses", totalExpenses);
        summary.put("balance", netBalance);
        summary.put("savings", totalIncome.subtract(totalExpenses).max(BigDecimal.ZERO));
        summary.put("currency", user.getCurrency());
        summary.put("month", now.getMonth().name() + " " + now.getYear());
        summary.put("recentTransactions", recentTransactions.stream()
                .map(t -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", t.getId());
                    entry.put("amount", t.getAmount());
                    entry.put("type", t.getType());
                    entry.put("category", Map.of("id", t.getCategory().getId(), "name", t.getCategory().getName()));
                    entry.put("description", t.getDescription());
                    entry.put("date", t.getDate());
                    return entry;
                })
                .toList());
        summary.put("activeBudgetAlerts", alerts);
        summary.put("recentSpending", recentSpending);
        summary.put("categoryBreakdown", breakdown.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> (BigDecimal) r[1]
                )));

        return summary;
    }

    public List<Map<String, Object>> getDistribution(UUID userId, LocalDate from, LocalDate to) {
        return transactionRepository.sumByCategory(userId, from, to).stream()
                .map(r -> Map.of("category", r[0], "amount", r[1]))
                .toList();
    }

    public List<Map<String, Object>> getMonthlyTrend(UUID userId, LocalDate from, LocalDate to) {
        return transactionRepository.sumByMonth(userId, from, to).stream()
                .map(r -> Map.of("year", r[0], "month", r[1], "type", r[2], "amount", r[3]))
                .toList();
    }

    public List<Map<String, Object>> getDailyTrend(UUID userId, LocalDate from, LocalDate to) {
        return transactionRepository.sumByDay(userId, from, to).stream()
                .map(r -> Map.of("date", r[0], "type", r[1], "amount", r[2]))
                .toList();
    }

    public Map<String, Object> getSummaryStatistics(UUID userId, LocalDate from, LocalDate to) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Object[]> sums = transactionRepository.sumByType(userId, from, to);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Object[] row : sums) {
            TransactionType t = (TransactionType) row[0];
            BigDecimal v = (BigDecimal) row[1];
            if (t == TransactionType.INCOME) totalIncome = v;
            else if (t == TransactionType.EXPENSE) totalExpenses = v;
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        // Find largest single expense
        List<Transaction> expenseTransactions = transactionRepository
                .findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(userId, TransactionType.EXPENSE, from, to);
        Map<String, Object> largestExpense = null;
        if (!expenseTransactions.isEmpty()) {
            Transaction max = expenseTransactions.get(0);
            largestExpense = new HashMap<>();
            largestExpense.put("amount", max.getAmount());
            largestExpense.put("description", max.getDescription());
            largestExpense.put("date", max.getDate());
        }

        long daysBetween = ChronoUnit.DAYS.between(from, to) + 1;
        BigDecimal averageDailySpend = daysBetween > 0
                ? totalExpenses.divide(BigDecimal.valueOf(daysBetween), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long transactionCount = expenseTransactions.size() +
                transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(userId, TransactionType.INCOME, from, to).size();

        Map<String, Object> result = new HashMap<>();
        result.put("totalIncome", totalIncome);
        result.put("totalExpenses", totalExpenses);
        result.put("netBalance", netBalance);
        result.put("largestExpense", largestExpense);
        result.put("transactionCount", transactionCount);
        result.put("averageDailySpend", averageDailySpend);
        result.put("currency", user.getCurrency());
        return result;
    }

    public String exportTransactionsCsv(UUID userId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Category,Description,Amount\n");

        for (Transaction t : transactions) {
            csv.append(t.getDate()).append(",");
            csv.append(t.getType()).append(",");
            csv.append(t.getCategory().getName()).append(",");
            csv.append(escapeCsv(t.getDescription())).append(",");
            csv.append(t.getAmount()).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
