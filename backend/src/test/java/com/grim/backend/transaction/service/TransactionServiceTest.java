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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    // ──────────────────────────────────────────────────────────────
    // Dependencies
    // ──────────────────────────────────────────────────────────────

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<PageRequest> pageRequestCaptor;

    // ──────────────────────────────────────────────────────────────
    // Shared test data
    // ──────────────────────────────────────────────────────────────

    private UUID userId;
    private UUID otherUserId;
    private UUID categoryId;
    private UUID transactionId;
    private User testUser;
    private User otherUser;
    private Category testCategory;
    private Transaction testTransaction;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
        testDate = LocalDate.of(2026, 6, 15);

        testUser = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .currency("USD")
                .build();

        otherUser = User.builder()
                .id(otherUserId)
                .name("Other User")
                .email("other@example.com")
                .currency("EUR")
                .build();

        testCategory = Category.builder()
                .id(categoryId)
                .name("Groceries")
                .user(testUser)
                .isDefault(false)
                .build();

        testTransaction = Transaction.builder()
                .id(transactionId)
                .user(testUser)
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.EXPENSE)
                .category(testCategory)
                .description("Weekly groceries")
                .date(testDate)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  createTransaction
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTransaction()")
    class CreateTransaction {

        @Test
        @DisplayName("should create transaction successfully for valid user and category")
        void success() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findByIdAndUser(categoryId, userId)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            Transaction result = transactionService.createTransaction(
                    userId,
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    categoryId,
                    "Weekly groceries",
                    testDate
            );

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(transactionId);
            assertThat(result.getUser().getId()).isEqualTo(userId);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.getCategory().getId()).isEqualTo(categoryId);
            assertThat(result.getDescription()).isEqualTo("Weekly groceries");
            assertThat(result.getDate()).isEqualTo(testDate);

            verify(userRepository).findById(userId);
            verify(categoryRepository).findByIdAndUser(categoryId, userId);
            verify(transactionRepository).save(any(Transaction.class));
            verify(transactionRepository).flush();
            verify(budgetService).checkBudgetAfterTransaction(userId, categoryId, testDate);
        }

        @Test
        @DisplayName("should create transaction for INCOME type")
        void success_incomeType() {
            Transaction incomeTransaction = Transaction.builder()
                    .id(transactionId)
                    .user(testUser)
                    .amount(new BigDecimal("5000.00"))
                    .type(TransactionType.INCOME)
                    .category(testCategory)
                    .description("Monthly salary")
                    .date(testDate)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findByIdAndUser(categoryId, userId)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(incomeTransaction);

            Transaction result = transactionService.createTransaction(
                    userId,
                    new BigDecimal("5000.00"),
                    TransactionType.INCOME,
                    categoryId,
                    "Monthly salary",
                    testDate
            );

            assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));

            verify(transactionRepository).save(any(Transaction.class));
            verify(transactionRepository).flush();
            verify(budgetService).checkBudgetAfterTransaction(userId, categoryId, testDate);
        }

        @Test
        @DisplayName("should create transaction with null description")
        void success_nullDescription() {
            Transaction transactionWithNullDesc = Transaction.builder()
                    .id(transactionId)
                    .user(testUser)
                    .amount(new BigDecimal("100.00"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description(null)
                    .date(testDate)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findByIdAndUser(categoryId, userId)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionWithNullDesc);

            Transaction result = transactionService.createTransaction(
                    userId,
                    new BigDecimal("100.00"),
                    TransactionType.EXPENSE,
                    categoryId,
                    null,
                    testDate
            );

            assertThat(result.getDescription()).isNull();
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user does not exist")
        void throws_whenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createTransaction(
                    userId,
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    categoryId,
                    "Weekly groceries",
                    testDate
            ))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findById(userId);
            verify(categoryRepository, never()).findByIdAndUser(any(), any());
            verify(transactionRepository, never()).save(any());
            verify(transactionRepository, never()).flush();
            verifyNoInteractions(budgetService);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found or not accessible")
        void throws_whenCategoryNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findByIdAndUser(categoryId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createTransaction(
                    userId,
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    categoryId,
                    "Weekly groceries",
                    testDate
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found or not accessible");

            verify(userRepository).findById(userId);
            verify(categoryRepository).findByIdAndUser(categoryId, userId);
            verify(transactionRepository, never()).save(any());
            verify(transactionRepository, never()).flush();
            verify(budgetService, never()).checkBudgetAfterTransaction(any(), any(), any());
        }

        @Test
        @DisplayName("should propagate exception when budgetService.checkBudgetAfterTransaction fails")
        void throwsWhenBudgetCheckFails() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findByIdAndUser(categoryId, userId)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
            doThrow(new RuntimeException("Budget service unavailable"))
                    .when(budgetService).checkBudgetAfterTransaction(userId, categoryId, testDate);

            assertThatThrownBy(() -> transactionService.createTransaction(
                    userId,
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    categoryId,
                    "Weekly groceries",
                    testDate
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Budget service unavailable");

            verify(transactionRepository).save(any(Transaction.class));
            verify(transactionRepository).flush();
            verify(budgetService).checkBudgetAfterTransaction(userId, categoryId, testDate);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getTransactionById
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTransactionById()")
    class GetTransactionById {

        @Test
        @DisplayName("should return transaction when found for the given user")
        void success() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));

            Transaction result = transactionService.getTransactionById(userId, transactionId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(transactionId);
            assertThat(result.getUser().getId()).isEqualTo(userId);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.getCategory().getId()).isEqualTo(categoryId);

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction does not exist")
        void throws_whenNotFound() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionById(userId, transactionId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found or not accessible");

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction belongs to different user (user isolation)")
        void throws_whenWrongUser() {
            UUID wrongUserId = UUID.randomUUID();
            when(transactionRepository.findByIdAndUserId(transactionId, wrongUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionById(wrongUserId, transactionId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found or not accessible");

            verify(transactionRepository).findByIdAndUserId(transactionId, wrongUserId);
            // Verify it never looked up by the correct user
            verify(transactionRepository, never()).findByIdAndUserId(transactionId, userId);
        }

        @Test
        @DisplayName("should not leak another user's transaction data")
        void userIsolation_noLeak() {
            // Simulate a transaction that belongs to otherUser being queried by testUser
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionById(userId, transactionId))
                    .isInstanceOf(ResourceNotFoundException.class);

            // Even though the transaction exists, it's not returned for the wrong user
            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getTransactions
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTransactions()")
    class GetTransactions {

        private Transaction transaction2;

        @BeforeEach
        void setUp() {
            transaction2 = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("75.50"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description("Lunch")
                    .date(LocalDate.of(2026, 6, 14))
                    .build();
        }

        @Test
        @DisplayName("should return paginated transactions with all filters applied")
        void withAllFilters() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            BigDecimal minAmount = new BigDecimal("10.00");
            BigDecimal maxAmount = new BigDecimal("1000.00");

            List<Transaction> transactions = List.of(testTransaction, transaction2);
            Page<Transaction> transactionPage = new PageImpl<>(transactions);

            when(transactionRepository.findFiltered(
                    eq(userId), eq(from), eq(to), eq(TransactionType.EXPENSE),
                    eq(categoryId), eq(minAmount), eq(maxAmount), eq("groceries"),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, from, to, TransactionType.EXPENSE, categoryId,
                    minAmount, maxAmount, "groceries", 1, 20
            );

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getId()).isEqualTo(transactionId);

            verify(transactionRepository).findFiltered(
                    eq(userId), eq(from), eq(to), eq(TransactionType.EXPENSE),
                    eq(categoryId), eq(minAmount), eq(maxAmount), eq("groceries"),
                    any(PageRequest.class)
            );
        }

        @Test
        @DisplayName("should return paginated transactions with no filters (all null)")
        void withNoFilters() {
            List<Transaction> transactions = List.of(testTransaction, transaction2);
            Page<Transaction> transactionPage = new PageImpl<>(transactions);

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, null, null, null, null,
                    null, null, null, 1, 20
            );

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);

            verify(transactionRepository).findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            );
        }

        @Test
        @DisplayName("should return empty page when no transactions match filters")
        void returnsEmptyPage() {
            Page<Transaction> emptyPage = Page.empty();

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), eq(TransactionType.INCOME),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(emptyPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, null, null, TransactionType.INCOME, null,
                    null, null, null, 1, 20
            );

            assertThat(result).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should use correct pagination parameters (page - 1 offset)")
        void usesCorrectPageOffset() {
            Page<Transaction> emptyPage = Page.empty();

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(emptyPage);

            transactionService.getTransactions(userId, null, null, null, null,
                    null, null, null, 3, 10);

            // Page 3 => PageRequest.of(2, 10) with Sort.by("date").descending()
            verify(transactionRepository).findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    argThat(pageable -> {
                        PageRequest pr = (PageRequest) pageable;
                        return pr.getPageNumber() == 2
                                && pr.getPageSize() == 10
                                && pr.getSort().equals(Sort.by("date").descending());
                    })
            );
        }

        @Test
        @DisplayName("should use page 0 offset for page 1")
        void usesPageZeroOffsetForPageOne() {
            Page<Transaction> emptyPage = Page.empty();

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(emptyPage);

            transactionService.getTransactions(userId, null, null, null, null,
                    null, null, null, 1, 25);

            verify(transactionRepository).findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    argThat(pageable -> {
                        PageRequest pr = (PageRequest) pageable;
                        return pr.getPageNumber() == 0
                                && pr.getPageSize() == 25
                                && pr.getSort().equals(Sort.by("date").descending());
                    })
            );
        }

        @Test
        @DisplayName("should use high page offset correctly")
        void usesCorrectPageOffsetForHighPage() {
            Page<Transaction> emptyPage = Page.empty();

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(emptyPage);

            transactionService.getTransactions(userId, null, null, null, null,
                    null, null, null, 50, 5);

            verify(transactionRepository).findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    argThat(pageable -> {
                        PageRequest pr = (PageRequest) pageable;
                        return pr.getPageNumber() == 49
                                && pr.getPageSize() == 5
                                && pr.getSort().equals(Sort.by("date").descending());
                    })
            );
        }

        @Test
        @DisplayName("should filter by date range only")
        void withDateRangeOnly() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), eq(from), eq(to), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, from, to, null, null,
                    null, null, null, 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by amount range only")
        void withAmountRangeOnly() {
            BigDecimal min = new BigDecimal("100.00");
            BigDecimal max = new BigDecimal("200.00");
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), eq(min), eq(max), isNull(),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, null, null, null, null,
                    min, max, null, 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAmount())
                    .isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should filter by search term only")
        void withSearchTerm() {
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), eq("groceries"),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, null, null, null, null,
                    null, null, "groceries", 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getDescription()).containsIgnoringCase("groceries");
        }

        @Test
        @DisplayName("should filter by category only")
        void withCategoryOnly() {
            UUID catId = UUID.randomUUID();
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    eq(catId), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, null, null, null, catId,
                    null, null, null, 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by type only")
        void withTypeOnly() {
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), eq(TransactionType.EXPENSE),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, null, null, TransactionType.EXPENSE, null,
                    null, null, null, 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by date range and type together")
        void withDateRangeAndType() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), eq(from), eq(to), eq(TransactionType.EXPENSE),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, from, to, TransactionType.EXPENSE, null,
                    null, null, null, 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by type and category together")
        void withTypeAndCategory() {
            UUID catId = UUID.randomUUID();
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), eq(TransactionType.EXPENSE),
                    eq(catId), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, null, null, TransactionType.EXPENSE, catId,
                    null, null, null, 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by amount and search term together")
        void withAmountAndSearch() {
            BigDecimal min = new BigDecimal("50.00");
            BigDecimal max = new BigDecimal("500.00");
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), eq(min), eq(max), eq("groceries"),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, null, null, null, null,
                    min, max, "groceries", 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by date range, category, and type together")
        void withDateRangeCategoryAndType() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            UUID catId = UUID.randomUUID();
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            when(transactionRepository.findFiltered(
                    eq(userId), eq(from), eq(to), eq(TransactionType.EXPENSE),
                    eq(catId), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(transactionPage);

            Page<Transaction> result = transactionService.getTransactions(
                    userId, from, to, TransactionType.EXPENSE, catId,
                    null, null, null, 1, 20
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should enforce user isolation by passing userId to repository")
        void userIsolation() {
            Page<Transaction> emptyPage = Page.empty();

            when(transactionRepository.findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            )).thenReturn(emptyPage);

            transactionService.getTransactions(userId, null, null, null, null,
                    null, null, null, 1, 20);

            // The userId is always passed — the repository query ensures user isolation
            verify(transactionRepository).findFiltered(
                    eq(userId), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)
            );
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updateTransaction
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTransaction()")
    class UpdateTransaction {

        @Test
        @DisplayName("should update all fields successfully")
        void fullUpdate() {
            UUID newCategoryId = UUID.randomUUID();
            Category newCategory = Category.builder()
                    .id(newCategoryId)
                    .name("Restaurants")
                    .user(testUser)
                    .build();
            LocalDate newDate = LocalDate.of(2026, 6, 20);

            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));
            when(categoryRepository.findByIdAndUser(newCategoryId, userId))
                    .thenReturn(Optional.of(newCategory));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Transaction result = transactionService.updateTransaction(
                    userId, transactionId,
                    new BigDecimal("200.00"),
                    TransactionType.INCOME,
                    newCategoryId,
                    "Updated description",
                    newDate
            );

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(result.getCategory().getId()).isEqualTo(newCategoryId);
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getDate()).isEqualTo(newDate);

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
            verify(categoryRepository).findByIdAndUser(newCategoryId, userId);
            verify(transactionRepository).save(testTransaction);
            verify(transactionRepository).flush();
            verify(budgetService).checkBudgetAfterTransaction(userId, newCategoryId, newDate);
        }

        @Test
        @DisplayName("should partially update only the provided fields (amount + description)")
        void partialUpdate_amountAndDescription() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Transaction result = transactionService.updateTransaction(
                    userId, transactionId,
                    new BigDecimal("299.99"),
                    null,
                    null,
                    "Updated description only",
                    null
            );

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("299.99"));
            assertThat(result.getDescription()).isEqualTo("Updated description only");
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.getCategory().getId()).isEqualTo(categoryId);
            assertThat(result.getDate()).isEqualTo(testDate);

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
            verify(categoryRepository, never()).findByIdAndUser(any(), any());
            verify(transactionRepository).save(testTransaction);
            verify(transactionRepository).flush();
            verify(budgetService).checkBudgetAfterTransaction(userId, categoryId, testDate);
        }

        @Test
        @DisplayName("should update only the type when other fields are null")
        void updateOnlyType() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Transaction result = transactionService.updateTransaction(
                    userId, transactionId,
                    null,
                    TransactionType.INCOME,
                    null,
                    null,
                    null
            );

            assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(result.getDescription()).isEqualTo("Weekly groceries");
            assertThat(result.getDate()).isEqualTo(testDate);

            verify(categoryRepository, never()).findByIdAndUser(any(), any());
            verify(budgetService).checkBudgetAfterTransaction(userId, categoryId, testDate);
        }

        @Test
        @DisplayName("should update only date when other fields are null")
        void updateOnlyDate() {
            LocalDate newDate = LocalDate.of(2026, 7, 1);

            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Transaction result = transactionService.updateTransaction(
                    userId, transactionId,
                    null, null, null, null, newDate
            );

            assertThat(result.getDate()).isEqualTo(newDate);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);

            verify(categoryRepository, never()).findByIdAndUser(any(), any());
            verify(budgetService).checkBudgetAfterTransaction(userId, categoryId, newDate);
        }

        @Test
        @DisplayName("should do nothing when all fields are null")
        void updateAllNull() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Transaction result = transactionService.updateTransaction(
                    userId, transactionId,
                    null, null, null, null, null
            );

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.getDescription()).isEqualTo("Weekly groceries");
            assertThat(result.getDate()).isEqualTo(testDate);

            verify(categoryRepository, never()).findByIdAndUser(any(), any());
            verify(budgetService).checkBudgetAfterTransaction(userId, categoryId, testDate);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction does not exist")
        void throws_whenNotFound() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.updateTransaction(
                    userId, transactionId,
                    new BigDecimal("200.00"),
                    TransactionType.EXPENSE,
                    categoryId,
                    "Updated",
                    testDate
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found or not accessible");

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
            verify(categoryRepository, never()).findByIdAndUser(any(), any());
            verify(transactionRepository, never()).save(any());
            verify(transactionRepository, never()).flush();
            verify(budgetService, never()).checkBudgetAfterTransaction(any(), any(), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when new category is not found")
        void throws_whenCategoryNotFound() {
            UUID newCategoryId = UUID.randomUUID();

            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));
            when(categoryRepository.findByIdAndUser(newCategoryId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.updateTransaction(
                    userId, transactionId,
                    new BigDecimal("200.00"),
                    TransactionType.EXPENSE,
                    newCategoryId,
                    "Updated",
                    testDate
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found or not accessible");

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
            verify(categoryRepository).findByIdAndUser(newCategoryId, userId);
            verify(transactionRepository, never()).save(any());
            verify(transactionRepository, never()).flush();
            verify(budgetService, never()).checkBudgetAfterTransaction(any(), any(), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction belongs to different user (user isolation)")
        void throws_whenWrongUser() {
            when(transactionRepository.findByIdAndUserId(transactionId, otherUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.updateTransaction(
                    otherUserId, transactionId,
                    new BigDecimal("200.00"),
                    TransactionType.EXPENSE,
                    categoryId,
                    "Updated",
                    testDate
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found or not accessible");

            verify(transactionRepository).findByIdAndUserId(transactionId, otherUserId);
            verify(transactionRepository, never()).save(any());
            verify(categoryRepository, never()).findByIdAndUser(any(), any());
            verify(budgetService, never()).checkBudgetAfterTransaction(any(), any(), any());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  deleteTransaction
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTransaction()")
    class DeleteTransaction {

        @Test
        @DisplayName("should delete transaction successfully when found")
        void success() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));

            transactionService.deleteTransaction(userId, transactionId);

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
            verify(transactionRepository).delete(testTransaction);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction does not exist")
        void throws_whenNotFound() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.deleteTransaction(userId, transactionId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found or not accessible");

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
            verify(transactionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction belongs to a different user (user isolation)")
        void throws_whenWrongUser() {
            UUID wrongUserId = UUID.randomUUID();

            when(transactionRepository.findByIdAndUserId(transactionId, wrongUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.deleteTransaction(wrongUserId, transactionId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found or not accessible");

            verify(transactionRepository).findByIdAndUserId(transactionId, wrongUserId);
            verify(transactionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should propagate exception when repository delete fails")
        void throwsWhenDeleteFails() {
            when(transactionRepository.findByIdAndUserId(transactionId, userId))
                    .thenReturn(Optional.of(testTransaction));
            doThrow(new RuntimeException("Database error"))
                    .when(transactionRepository).delete(testTransaction);

            assertThatThrownBy(() -> transactionService.deleteTransaction(userId, transactionId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            verify(transactionRepository).findByIdAndUserId(transactionId, userId);
            verify(transactionRepository).delete(testTransaction);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getDashboardSummary
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDashboardSummary()")
    class GetDashboardSummary {

        private static final LocalDate FIXED_NOW = LocalDate.of(2026, 6, 25);
        private static final LocalDate FIXED_START_OF_MONTH = LocalDate.of(2026, 6, 1);
        private static final LocalDate FIXED_END_OF_MONTH = LocalDate.of(2026, 6, 30);
        private static final LocalDate FIXED_WEEK_AGO = LocalDate.of(2026, 6, 18);

        @Test
        @DisplayName("should return full dashboard summary for user with transactions and budgets")
        void success() {
            List<Object[]> sums = List.of(
                    new Object[]{TransactionType.INCOME, new BigDecimal("5000.00")},
                    new Object[]{TransactionType.EXPENSE, new BigDecimal("3200.00")}
            );
            List<Transaction> recent = List.of(testTransaction);
            List<Object[]> trend = List.of(
                    new Object[]{LocalDate.of(2026, 6, 24), TransactionType.EXPENSE, new BigDecimal("150.00")},
                    new Object[]{LocalDate.of(2026, 6, 23), TransactionType.EXPENSE, new BigDecimal("75.50")}
            );
            Map<String, Object> warningBudget = new HashMap<>();
            warningBudget.put("id", UUID.randomUUID());
            warningBudget.put("category", Map.of("id", categoryId, "name", "Groceries"));
            warningBudget.put("status", "warning");
            warningBudget.put("percentage", new BigDecimal("85.0000"));
            warningBudget.put("spent", new BigDecimal("425.00"));
            warningBudget.put("limitAmount", new BigDecimal("500.00"));

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(transactionRepository.sumByType(userId, FIXED_START_OF_MONTH, FIXED_END_OF_MONTH))
                        .thenReturn(sums);
                when(transactionRepository.findTop5ByUserIdOrderByDateDesc(userId))
                        .thenReturn(recent);
                when(transactionRepository.sumByDay(userId, FIXED_WEEK_AGO, FIXED_NOW))
                        .thenReturn(trend);
                when(budgetService.getBudgets(userId, null, null))
                        .thenReturn(List.of(warningBudget));

                Map<String, Object> result = transactionService.getDashboardSummary(userId);

                assertThat(result)
                        .containsEntry("totalIncome", new BigDecimal("5000.00"))
                        .containsEntry("totalExpenses", new BigDecimal("3200.00"))
                        .containsEntry("balance", new BigDecimal("1800.00"))
                        .containsEntry("savings", new BigDecimal("1800.00"))
                        .containsEntry("currency", "USD")
                        .containsEntry("month", "JUNE 2026")
                        .containsKey("recentTransactions")
                        .containsKey("activeBudgetAlerts")
                        .containsKey("recentSpending");

                assertThat(result.get("activeBudgetAlerts")).asList().hasSize(1);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> alerts = (List<Map<String, Object>>) result.get("activeBudgetAlerts");
                assertThat(alerts.get(0))
                        .containsEntry("status", "warning")
                        .containsEntry("categoryName", "Groceries")
                        .containsKey("budgetId")
                        .containsKey("percentage")
                        .containsKey("spent")
                        .containsKey("limit");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recentSpending =
                        (List<Map<String, Object>>) result.get("recentSpending");
                assertThat(recentSpending).hasSize(2);

                verify(userRepository).findById(userId);
                verify(transactionRepository).sumByType(userId, FIXED_START_OF_MONTH, FIXED_END_OF_MONTH);
                verify(transactionRepository).findTop5ByUserIdOrderByDateDesc(userId);
                verify(transactionRepository).sumByDay(userId, FIXED_WEEK_AGO, FIXED_NOW);
                verify(budgetService).getBudgets(userId, null, null);
        }

        @Test
        @DisplayName("should return zero income and expenses when no transactions exist")
        void noTransactions() {

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(transactionRepository.sumByType(userId, FIXED_START_OF_MONTH, FIXED_END_OF_MONTH))
                        .thenReturn(List.of());
                when(transactionRepository.findTop5ByUserIdOrderByDateDesc(userId))
                        .thenReturn(List.of());
                when(transactionRepository.sumByDay(userId, FIXED_WEEK_AGO, FIXED_NOW))
                        .thenReturn(List.of());
                when(budgetService.getBudgets(userId, null, null))
                        .thenReturn(List.of());

                Map<String, Object> result = transactionService.getDashboardSummary(userId);

                assertThat(result)
                        .containsEntry("totalIncome", BigDecimal.ZERO)
                        .containsEntry("totalExpenses", BigDecimal.ZERO)
                        .containsEntry("balance", BigDecimal.ZERO)
                        .containsEntry("savings", BigDecimal.ZERO);
                assertThat(result.get("activeBudgetAlerts")).asList().isEmpty();
                assertThat(result.get("recentTransactions")).asList().isEmpty();
                assertThat(result.get("recentSpending")).asList().isEmpty();
        }

        @Test
        @DisplayName("should filter out budgets with 'ok' status from alerts")
        void onlyNonOkBudgetsInAlerts() {
            Map<String, Object> okBudget = new HashMap<>();
            okBudget.put("status", "ok");

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(transactionRepository.sumByType(userId, FIXED_START_OF_MONTH, FIXED_END_OF_MONTH))
                        .thenReturn(List.of());
                when(transactionRepository.findTop5ByUserIdOrderByDateDesc(userId))
                        .thenReturn(List.of());
                when(transactionRepository.sumByDay(userId, FIXED_WEEK_AGO, FIXED_NOW))
                        .thenReturn(List.of());
                when(budgetService.getBudgets(userId, null, null))
                        .thenReturn(List.of(okBudget));

                Map<String, Object> result = transactionService.getDashboardSummary(userId);

                assertThat(result.get("activeBudgetAlerts")).asList().isEmpty();
        }

        @Test
        @DisplayName("should include exceeded budgets in alerts")
        void exceededBudgetInAlerts() {
            Map<String, Object> exceededBudget = new HashMap<>();
            exceededBudget.put("id", UUID.randomUUID());
            exceededBudget.put("category", Map.of("id", categoryId, "name", "Groceries"));
            exceededBudget.put("status", "exceeded");
            exceededBudget.put("percentage", new BigDecimal("120.0000"));
            exceededBudget.put("spent", new BigDecimal("600.00"));
            exceededBudget.put("limitAmount", new BigDecimal("500.00"));

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(transactionRepository.sumByType(userId, FIXED_START_OF_MONTH, FIXED_END_OF_MONTH))
                        .thenReturn(List.of());
                when(transactionRepository.findTop5ByUserIdOrderByDateDesc(userId))
                        .thenReturn(List.of());
                when(transactionRepository.sumByDay(userId, FIXED_WEEK_AGO, FIXED_NOW))
                        .thenReturn(List.of());
                when(budgetService.getBudgets(userId, null, null))
                        .thenReturn(List.of(exceededBudget));

                Map<String, Object> result = transactionService.getDashboardSummary(userId);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> alerts = (List<Map<String, Object>>) result.get("activeBudgetAlerts");
                assertThat(alerts).hasSize(1);
                assertThat(alerts.get(0))
                        .containsEntry("status", "exceeded")
                        .containsEntry("categoryName", "Groceries");
        }

        @Test
        @DisplayName("should only include EXPENSE type in recentSpending")
        void recentSpendingOnlyExpenses() {
            List<Object[]> trend = List.of(
                    new Object[]{LocalDate.of(2026, 6, 24), TransactionType.EXPENSE, new BigDecimal("150.00")},
                    new Object[]{LocalDate.of(2026, 6, 23), TransactionType.INCOME, new BigDecimal("5000.00")}
            );

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(transactionRepository.sumByType(userId, FIXED_START_OF_MONTH, FIXED_END_OF_MONTH))
                        .thenReturn(List.of());
                when(transactionRepository.findTop5ByUserIdOrderByDateDesc(userId))
                        .thenReturn(List.of());
                when(transactionRepository.sumByDay(userId, FIXED_WEEK_AGO, FIXED_NOW))
                        .thenReturn(trend);
                when(budgetService.getBudgets(userId, null, null))
                        .thenReturn(List.of());

                Map<String, Object> result = transactionService.getDashboardSummary(userId);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recentSpending =
                        (List<Map<String, Object>>) result.get("recentSpending");
                assertThat(recentSpending).hasSize(1);
                assertThat(recentSpending.get(0)).containsEntry("amount", new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should have savings as zero when expenses exceed income")
        void savingsZeroWhenNegative() {
            List<Object[]> sums = List.of(
                    new Object[]{TransactionType.INCOME, new BigDecimal("1000.00")},
                    new Object[]{TransactionType.EXPENSE, new BigDecimal("1500.00")}
            );

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(transactionRepository.sumByType(userId, FIXED_START_OF_MONTH, FIXED_END_OF_MONTH))
                        .thenReturn(sums);
                when(transactionRepository.findTop5ByUserIdOrderByDateDesc(userId))
                        .thenReturn(List.of());
                when(transactionRepository.sumByDay(userId, FIXED_WEEK_AGO, FIXED_NOW))
                        .thenReturn(List.of());
                when(budgetService.getBudgets(userId, null, null))
                        .thenReturn(List.of());

                Map<String, Object> result = transactionService.getDashboardSummary(userId);

                assertThat(result)
                        .containsEntry("balance", new BigDecimal("-500.00"))
                        .containsEntry("savings", BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user not found")
        void throws_whenUserNotFound() {
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> transactionService.getDashboardSummary(userId))
                        .isInstanceOf(UsernameNotFoundException.class)
                        .hasMessage("User not found");

                verify(transactionRepository, never()).sumByType(any(), any(), any());
                verify(transactionRepository, never()).findTop5ByUserIdOrderByDateDesc(any());
                verify(transactionRepository, never()).sumByDay(any(), any(), any());
                verifyNoInteractions(budgetService);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getDistribution
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDistribution()")
    class GetDistribution {

        @Test
        @DisplayName("should return distribution grouped by category")
        void success() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);
            List<Object[]> raw = List.of(
                    new Object[]{"Groceries", new BigDecimal("1500.00")},
                    new Object[]{"Rent", new BigDecimal("12000.00")},
                    new Object[]{"Transport", new BigDecimal("500.00")}
            );

            when(transactionRepository.sumByCategory(userId, from, to)).thenReturn(raw);

            List<Map<String, Object>> result = transactionService.getDistribution(userId, from, to);

            assertThat(result).hasSize(3);
            assertThat(result.get(0))
                    .containsEntry("category", "Groceries")
                    .containsEntry("amount", new BigDecimal("1500.00"));
            assertThat(result.get(1))
                    .containsEntry("category", "Rent")
                    .containsEntry("amount", new BigDecimal("12000.00"));
            assertThat(result.get(2))
                    .containsEntry("category", "Transport")
                    .containsEntry("amount", new BigDecimal("500.00"));

            verify(transactionRepository).sumByCategory(userId, from, to);
        }

        @Test
        @DisplayName("should return empty list when no transactions in period")
        void emptyResult() {
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 12, 31);

            when(transactionRepository.sumByCategory(userId, from, to)).thenReturn(List.of());

            List<Map<String, Object>> result = transactionService.getDistribution(userId, from, to);

            assertThat(result).isEmpty();
            verify(transactionRepository).sumByCategory(userId, from, to);
        }

        @Test
        @DisplayName("should handle single category distribution")
        void singleCategory() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            List<Object[]> raw = java.util.Collections.singletonList(
                    new Object[]{"Groceries", new BigDecimal("150.00")}
            );

            when(transactionRepository.sumByCategory(userId, from, to)).thenReturn(raw);

            List<Map<String, Object>> result = transactionService.getDistribution(userId, from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                    .containsEntry("category", "Groceries")
                    .containsEntry("amount", new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should pass correct userId for user isolation")
        void userIsolation() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);

            when(transactionRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());

            transactionService.getDistribution(userId, from, to);

            verify(transactionRepository).sumByCategory(userId, from, to);
            verify(transactionRepository, never()).sumByCategory(otherUserId, from, to);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getMonthlyTrend
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMonthlyTrend()")
    class GetMonthlyTrend {

        @Test
        @DisplayName("should return monthly trend grouped by year, month, type")
        void success() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);
            List<Object[]> raw = List.of(
                    new Object[]{2026, 6, TransactionType.INCOME, new BigDecimal("5000.00")},
                    new Object[]{2026, 6, TransactionType.EXPENSE, new BigDecimal("3200.00")},
                    new Object[]{2026, 5, TransactionType.INCOME, new BigDecimal("4500.00")},
                    new Object[]{2026, 5, TransactionType.EXPENSE, new BigDecimal("2800.00")}
            );

            when(transactionRepository.sumByMonth(userId, from, to)).thenReturn(raw);

            List<Map<String, Object>> result = transactionService.getMonthlyTrend(userId, from, to);

            assertThat(result).hasSize(4);
            assertThat(result.get(0))
                    .containsEntry("year", 2026)
                    .containsEntry("month", 6)
                    .containsEntry("type", TransactionType.INCOME)
                    .containsEntry("amount", new BigDecimal("5000.00"));

            verify(transactionRepository).sumByMonth(userId, from, to);
        }

        @Test
        @DisplayName("should return empty list when no data in range")
        void emptyResult() {
            LocalDate from = LocalDate.of(2020, 1, 1);
            LocalDate to = LocalDate.of(2020, 12, 31);

            when(transactionRepository.sumByMonth(userId, from, to)).thenReturn(List.of());

            List<Map<String, Object>> result = transactionService.getMonthlyTrend(userId, from, to);

            assertThat(result).isEmpty();
            verify(transactionRepository).sumByMonth(userId, from, to);
        }

        @Test
        @DisplayName("should handle single month with both types")
        void singleMonthBothTypes() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            List<Object[]> raw = List.of(
                    new Object[]{2026, 6, TransactionType.INCOME, new BigDecimal("5000.00")},
                    new Object[]{2026, 6, TransactionType.EXPENSE, new BigDecimal("1500.00")}
            );

            when(transactionRepository.sumByMonth(userId, from, to)).thenReturn(raw);

            List<Map<String, Object>> result = transactionService.getMonthlyTrend(userId, from, to);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .containsEntry("year", 2026)
                    .containsEntry("month", 6)
                    .containsEntry("type", TransactionType.INCOME)
                    .containsEntry("amount", new BigDecimal("5000.00"));
            assertThat(result.get(1))
                    .containsEntry("year", 2026)
                    .containsEntry("month", 6)
                    .containsEntry("type", TransactionType.EXPENSE)
                    .containsEntry("amount", new BigDecimal("1500.00"));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getDailyTrend
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDailyTrend()")
    class GetDailyTrend {

        @Test
        @DisplayName("should return daily trend grouped by date and type")
        void success() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            List<Object[]> raw = List.of(
                    new Object[]{LocalDate.of(2026, 6, 15), TransactionType.EXPENSE, new BigDecimal("150.00")},
                    new Object[]{LocalDate.of(2026, 6, 15), TransactionType.INCOME, new BigDecimal("5000.00")},
                    new Object[]{LocalDate.of(2026, 6, 14), TransactionType.EXPENSE, new BigDecimal("75.50")}
            );

            when(transactionRepository.sumByDay(userId, from, to)).thenReturn(raw);

            List<Map<String, Object>> result = transactionService.getDailyTrend(userId, from, to);

            assertThat(result).hasSize(3);
            assertThat(result.get(0))
                    .containsEntry("date", LocalDate.of(2026, 6, 15))
                    .containsEntry("type", TransactionType.EXPENSE)
                    .containsEntry("amount", new BigDecimal("150.00"));

            verify(transactionRepository).sumByDay(userId, from, to);
        }

        @Test
        @DisplayName("should return empty list when no data in range")
        void emptyResult() {
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 12, 31);

            when(transactionRepository.sumByDay(userId, from, to)).thenReturn(List.of());

            List<Map<String, Object>> result = transactionService.getDailyTrend(userId, from, to);

            assertThat(result).isEmpty();
            verify(transactionRepository).sumByDay(userId, from, to);
        }

        @Test
        @DisplayName("should handle single day with multiple entries")
        void singleDayMultipleTypes() {
            LocalDate day = LocalDate.of(2026, 6, 15);
            List<Object[]> raw = List.of(
                    new Object[]{day, TransactionType.EXPENSE, new BigDecimal("150.00")},
                    new Object[]{day, TransactionType.EXPENSE, new BigDecimal("75.00")}
            );

            when(transactionRepository.sumByDay(userId, day, day)).thenReturn(raw);

            List<Map<String, Object>> result = transactionService.getDailyTrend(userId, day, day);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .containsEntry("date", day)
                    .containsEntry("type", TransactionType.EXPENSE)
                    .containsEntry("amount", new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should pass correct userId for user isolation")
        void userIsolation() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);

            when(transactionRepository.sumByDay(any(), any(), any())).thenReturn(List.of());

            transactionService.getDailyTrend(userId, from, to);

            verify(transactionRepository).sumByDay(userId, from, to);
            verify(transactionRepository, never()).sumByDay(otherUserId, from, to);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getSummaryStatistics
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSummaryStatistics()")
    class GetSummaryStatistics {

        @Test
        @DisplayName("should return full statistics for user with transactions")
        void success() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);

            List<Object[]> sums = List.of(
                    new Object[]{TransactionType.INCOME, new BigDecimal("60000.00")},
                    new Object[]{TransactionType.EXPENSE, new BigDecimal("42000.00")}
            );
            Transaction largestExpense = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("5000.00"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description("Rent payment")
                    .date(LocalDate.of(2026, 6, 1))
                    .build();
            List<Transaction> expenseTransactions = List.of(largestExpense, testTransaction);
            List<Transaction> incomeTransactions = List.of(
                    Transaction.builder().amount(new BigDecimal("60000.00")).build()
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(transactionRepository.sumByType(userId, from, to)).thenReturn(sums);
            when(transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(
                    userId, TransactionType.EXPENSE, from, to
            )).thenReturn(expenseTransactions);
            when(transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(
                    userId, TransactionType.INCOME, from, to
            )).thenReturn(incomeTransactions);

            Map<String, Object> result = transactionService.getSummaryStatistics(userId, from, to);

            assertThat(result)
                    .containsEntry("totalIncome", new BigDecimal("60000.00"))
                    .containsEntry("totalExpenses", new BigDecimal("42000.00"))
                    .containsEntry("netBalance", new BigDecimal("18000.00"))
                    .containsEntry("transactionCount", 3L)
                    .containsEntry("currency", "USD");

            assertThat(result).containsKey("largestExpense");
            @SuppressWarnings("unchecked")
            Map<String, Object> largest = (Map<String, Object>) result.get("largestExpense");
            assertThat(largest)
                    .containsEntry("amount", new BigDecimal("5000.00"))
                    .containsEntry("description", "Rent payment")
                    .containsEntry("date", LocalDate.of(2026, 6, 1));

            // daysBetween = 365, averageDailySpend = 42000 / 365 = 115.07 (rounded to 2 decimals)
            BigDecimal expectedAvg = new BigDecimal("42000.00")
                    .divide(new BigDecimal("365"), 2, java.math.RoundingMode.HALF_UP);
            assertThat(result.get("averageDailySpend")).isEqualTo(expectedAvg);
        }

        @Test
        @DisplayName("should return null largestExpense when no expenses exist")
        void noExpenses() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);

            List<Object[]> sums = java.util.Collections.singletonList(
                    new Object[]{TransactionType.INCOME, new BigDecimal("5000.00")}
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(transactionRepository.sumByType(userId, from, to)).thenReturn(sums);
            when(transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(
                    userId, TransactionType.EXPENSE, from, to
            )).thenReturn(List.of());
            when(transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(
                    userId, TransactionType.INCOME, from, to
            )).thenReturn(List.of());

            Map<String, Object> result = transactionService.getSummaryStatistics(userId, from, to);

            assertThat(result)
                    .containsEntry("totalIncome", new BigDecimal("5000.00"))
                    .containsEntry("totalExpenses", BigDecimal.ZERO)
                    .containsEntry("netBalance", new BigDecimal("5000.00"))
                    .containsEntry("transactionCount", 0L)
                    .containsEntry("largestExpense", null)
                    .containsEntry("averageDailySpend", new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("should handle single-day range correctly")
        void singleDayRange() {
            LocalDate day = LocalDate.of(2026, 6, 15);

            List<Object[]> sums = java.util.Collections.singletonList(
                    new Object[]{TransactionType.EXPENSE, new BigDecimal("100.00")}
            );
            List<Transaction> expenseTransactions = List.of(testTransaction);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(transactionRepository.sumByType(userId, day, day)).thenReturn(sums);
            when(transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(
                    userId, TransactionType.EXPENSE, day, day
            )).thenReturn(expenseTransactions);
            when(transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(
                    userId, TransactionType.INCOME, day, day
            )).thenReturn(List.of());

            Map<String, Object> result = transactionService.getSummaryStatistics(userId, day, day);

            // daysBetween = 1, so averageDailySpend = 100.00
            assertThat(result)
                    .containsEntry("averageDailySpend", new BigDecimal("100.00"))
                    .containsEntry("transactionCount", 1L);
        }

        @Test
        @DisplayName("should handle zero expenses for average daily spend")
        void zeroExpensesAverageDailySpend() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(transactionRepository.sumByType(userId, from, to)).thenReturn(List.of());
            when(transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(
                    userId, TransactionType.EXPENSE, from, to
            )).thenReturn(List.of());
            when(transactionRepository.findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(
                    userId, TransactionType.INCOME, from, to
            )).thenReturn(List.of());

            Map<String, Object> result = transactionService.getSummaryStatistics(userId, from, to);

            assertThat(result)
                    .containsEntry("averageDailySpend", new BigDecimal("0.00"))
                    .containsEntry("transactionCount", 0L);
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user not found")
        void throws_whenUserNotFound() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getSummaryStatistics(userId, from, to))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");

            verify(transactionRepository, never()).sumByType(any(), any(), any());
            verify(transactionRepository, never()).findByUserIdAndTypeAndDateBetweenOrderByAmountDesc(any(), any(), any(), any());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  exportTransactionsCsv
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("exportTransactionsCsv()")
    class ExportTransactionsCsv {

        @Test
        @DisplayName("should export transactions as CSV with header and data rows")
        void success() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);

            Transaction t1 = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("5000.00"))
                    .type(TransactionType.INCOME)
                    .category(testCategory)
                    .description("Salary")
                    .date(LocalDate.of(2026, 6, 1))
                    .build();

            Transaction t2 = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("150.00"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description("Groceries")
                    .date(LocalDate.of(2026, 6, 15))
                    .build();

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
                    .thenReturn(List.of(t1, t2));

            String csv = transactionService.exportTransactionsCsv(userId, from, to);

            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(3); // header + 2 data rows
            assertThat(lines[0]).isEqualTo("Date,Type,Category,Description,Amount");
            assertThat(lines[1]).contains("2026-06-01,INCOME,Groceries,Salary,5000.00");
            assertThat(lines[2]).contains("2026-06-15,EXPENSE,Groceries,Groceries,150.00");

            verify(transactionRepository).findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);
        }

        @Test
        @DisplayName("should return header only when no transactions in period")
        void emptyResult() {
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 12, 31);

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
                    .thenReturn(List.of());

            String csv = transactionService.exportTransactionsCsv(userId, from, to);

            assertThat(csv).isEqualTo("Date,Type,Category,Description,Amount\n");
        }

        @Test
        @DisplayName("should handle null description by outputting empty string")
        void nullDescription() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);

            Transaction t = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("100.00"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description(null)
                    .date(LocalDate.of(2026, 6, 15))
                    .build();

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
                    .thenReturn(List.of(t));

            String csv = transactionService.exportTransactionsCsv(userId, from, to);

            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(2);
            // The fourth column (Description) should be empty
            String[] columns = lines[1].split(",");
            assertThat(columns[3]).isEmpty();
        }

        @Test
        @DisplayName("should escape description containing commas")
        void escapeCommasInDescription() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);

            String descWithComma = "Groceries, Walmart, and Target";
            Transaction t = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("200.00"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description(descWithComma)
                    .date(LocalDate.of(2026, 6, 15))
                    .build();

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
                    .thenReturn(List.of(t));

            String csv = transactionService.exportTransactionsCsv(userId, from, to);

            String[] lines = csv.split("\n");
            assertThat(lines[1]).contains("\"Groceries, Walmart, and Target\"");
        }

        @Test
        @DisplayName("should escape description containing double quotes")
        void escapeQuotesInDescription() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);

            String descWithQuotes = "He said \"hello\"";
            Transaction t = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("50.00"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description(descWithQuotes)
                    .date(LocalDate.of(2026, 6, 15))
                    .build();

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
                    .thenReturn(List.of(t));

            String csv = transactionService.exportTransactionsCsv(userId, from, to);

            assertThat(csv).contains("\"He said \"\"hello\"\"\"");
        }

        @Test
        @DisplayName("should escape description containing newlines")
        void escapeNewlinesInDescription() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);

            String descWithNewline = "Line1\nLine2";
            Transaction t = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("75.00"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description(descWithNewline)
                    .date(LocalDate.of(2026, 6, 15))
                    .build();

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
                    .thenReturn(List.of(t));

            String csv = transactionService.exportTransactionsCsv(userId, from, to);

            assertThat(csv).contains("\"Line1\nLine2\"");
        }

        @Test
        @DisplayName("should not escape normal descriptions")
        void noEscapeForNormalDescriptions() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);

            Transaction t = Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .amount(new BigDecimal("100.00"))
                    .type(TransactionType.EXPENSE)
                    .category(testCategory)
                    .description("Normal description")
                    .date(LocalDate.of(2026, 6, 15))
                    .build();

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
                    .thenReturn(List.of(t));

            String csv = transactionService.exportTransactionsCsv(userId, from, to);

            assertThat(csv).contains("Normal description");
            // Should NOT be wrapped in quotes
            assertThat(csv).doesNotContain("\"Normal description\"");
        }

        @Test
        @DisplayName("should pass correct userId for user isolation")
        void userIsolation() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                    .thenReturn(List.of());

            transactionService.exportTransactionsCsv(userId, from, to);

            verify(transactionRepository).findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);
            verify(transactionRepository, never()).findByUserIdAndDateBetweenOrderByDateDesc(otherUserId, from, to);
        }
    }
}
