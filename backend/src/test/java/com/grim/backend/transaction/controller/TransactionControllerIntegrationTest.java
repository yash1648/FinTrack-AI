package com.grim.backend.transaction.controller;

import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.auth.security.JwtProvider;
import com.grim.backend.auth.service.EmailService;
import com.grim.backend.category.entity.Category;
import com.grim.backend.category.repository.CategoryRepository;
import com.grim.backend.transaction.dto.CreateTransactionRequest;
import com.grim.backend.transaction.dto.UpdateTransactionRequest;
import com.grim.backend.transaction.entity.TransactionType;
import com.grim.backend.transaction.repository.TransactionRepository;
import tools.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionControllerIntegrationTest {

    private static HttpComponentsClientHttpRequestFactory patchAwareFactory() {
        return new HttpComponentsClientHttpRequestFactory();
    }

    private final RestTemplate restTemplate = new RestTemplate(patchAwareFactory()) {
        @Override
        public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
                throws RestClientException {
            try {
                return super.getForEntity(url, responseType, uriVariables);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                @SuppressWarnings("unchecked")
                ResponseEntity<T> entity = (ResponseEntity<T>) ResponseEntity
                        .status(e.getStatusCode())
                        .body(e.getResponseBodyAsString());
                return entity;
            }
        }

        @Override
        public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Object... uriVariables)
                throws RestClientException {
            try {
                return super.postForEntity(url, request, responseType, uriVariables);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                @SuppressWarnings("unchecked")
                ResponseEntity<T> entity = (ResponseEntity<T>) ResponseEntity
                        .status(e.getStatusCode())
                        .body(e.getResponseBodyAsString());
                return entity;
            }
        }

        @Override
        public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
                HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
                throws RestClientException {
            try {
                return super.exchange(url, method, requestEntity, responseType, uriVariables);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                @SuppressWarnings("unchecked")
                ResponseEntity<T> entity = (ResponseEntity<T>) ResponseEntity
                        .status(e.getStatusCode())
                        .body(e.getResponseBodyAsString());
                return entity;
            }
        }
    };

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Category testCategory;
    private String jwtToken;
    private final LocalDate today = LocalDate.now();

    @TestConfiguration
    static class TestMockBeans {

        @SuppressWarnings("unchecked")
        @Bean
        @Primary
        RedisTemplate<String, Object> redisTemplate() {
            RedisTemplate<String, Object> template = mock(RedisTemplate.class);
            ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
            when(template.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn(null);
            when(valueOps.increment(anyString())).thenReturn(1L);
            return template;
        }

        @Bean
        @Primary
        EmailService emailService() {
            return mock(EmailService.class);
        }
    }

    @BeforeEach
    void setUp() {
        // Wipe all data in reverse-dependency order
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("transact@example.com")
                .name("Transaction Tester")
                .passwordHash("irrelevant-for-jwt-auth")
                .currency("USD")
                .emailVerified(true)
                .active(true)
                .build();
        testUser = userRepository.save(testUser);

        // Create test category
        testCategory = Category.builder()
                .name("Food & Dining")
                .user(testUser)
                .isDefault(false)
                .build();
        testCategory = categoryRepository.save(testCategory);

        // Generate a valid JWT for the test user
        jwtToken = jwtProvider.generateAccessToken(testUser.getId(), testUser.getEmail());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> jsonEntityWithAuth(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> authEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ──────────────────────────────────────────────
    // All endpoints — 401 without authentication
    // ──────────────────────────────────────────────

    @Test
    void all_ShouldReturn401_WhenNoAuth() {
        // GET list
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                url("/api/v1/transactions"), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = getResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(401);

        // POST create
        HttpEntity<String> postEntity = jsonEntity(write(
                new CreateTransactionRequest(BigDecimal.TEN, TransactionType.EXPENSE, UUID.randomUUID(), "test", today)));
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                url("/api/v1/transactions"), postEntity, String.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // GET by id
        ResponseEntity<String> getByIdResponse = restTemplate.getForEntity(
                url("/api/v1/transactions/{id}"), String.class, UUID.randomUUID());
        assertThat(getByIdResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // PATCH by id
        HttpEntity<String> patchEntity = jsonEntity("{}");
        ResponseEntity<String> patchResponse = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.PATCH, patchEntity, String.class, UUID.randomUUID());
        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // DELETE by id
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.DELETE,
                new HttpEntity<>(new HttpHeaders()), String.class, UUID.randomUUID());
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void all_ShouldReturn401_WhenBadToken() {
        String badToken = "garbage-jwt";

        // GET list
        ResponseEntity<String> getResponse = restTemplate.exchange(
                url("/api/v1/transactions"), HttpMethod.GET, authEntity(badToken), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = getResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(401);

        // POST create
        HttpEntity<String> postEntity = jsonEntityWithAuth(write(
                new CreateTransactionRequest(BigDecimal.TEN, TransactionType.EXPENSE, testCategory.getId(), "test", today)),
                badToken);
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                url("/api/v1/transactions"), postEntity, String.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/transactions — create
    // ──────────────────────────────────────────────

    @Test
    void createTransaction_ShouldReturn201_WhenValidExpense() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(250.00),
                TransactionType.EXPENSE,
                testCategory.getId(),
                "Lunch at restaurant",
                today
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(request), jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<Object>read(body, "$.data.amount")).isNotNull();
        assertThat(JsonPath.<String>read(body, "$.data.type")).isEqualTo("EXPENSE");
        assertThat(JsonPath.<String>read(body, "$.data.description")).isEqualTo("Lunch at restaurant");
        assertThat(JsonPath.<String>read(body, "$.data.date")).isEqualTo(today.toString());
        assertThat(JsonPath.<String>read(body, "$.data.category.id")).isEqualTo(testCategory.getId().toString());
        assertThat(JsonPath.<String>read(body, "$.data.category.name")).isEqualTo("Food & Dining");
        assertThat(JsonPath.<String>read(body, "$.data.userId")).isEqualTo(testUser.getId().toString());
        assertThat(JsonPath.<String>read(body, "$.data.id")).isNotNull();
    }

    @Test
    void createTransaction_ShouldReturn201_WhenValidIncome() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(5000.00),
                TransactionType.INCOME,
                testCategory.getId(),
                "Paycheck",
                today
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(request), jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.type")).isEqualTo("INCOME");
        assertThat(JsonPath.<String>read(body, "$.data.description")).isEqualTo("Paycheck");
        assertThat(JsonPath.<String>read(body, "$.data.category.name")).isEqualTo("Food & Dining");
    }

    @Test
    void createTransaction_ShouldReturn201_WhenNoDescription() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(100),
                TransactionType.INCOME,
                testCategory.getId(),
                null,
                today
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(request), jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Object>read(body, "$.data.description")).isNull();
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/transactions — validation errors
    // ──────────────────────────────────────────────

    @Test
    void createTransaction_ShouldReturn422_WhenInvalidBody() {
        String invalidJson = """
                {
                    "amount": -10,
                    "type": "NOT_A_TYPE",
                    "categoryId": null
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(invalidJson, jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    @Test
    void createTransaction_ShouldReturn422_WhenAmountNegative() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(-50),
                TransactionType.EXPENSE,
                testCategory.getId(),
                "Negative amount",
                today
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(request), jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    @Test
    void createTransaction_ShouldReturn422_WhenAmountNull() {
        String json = """
                {
                    "type": "EXPENSE",
                    "categoryId": "%s",
                    "date": "%s"
                }
                """.formatted(testCategory.getId(), today);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(json, jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    @Test
    void createTransaction_ShouldReturn422_WhenCategoryNotExists() {
        UUID fakeCategoryId = UUID.randomUUID();
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(100),
                TransactionType.EXPENSE,
                fakeCategoryId,
                "Bad category",
                today
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(request), jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(404);
    }

    @Test
    void createTransaction_ShouldReturn422_WhenFutureDate() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(100),
                TransactionType.EXPENSE,
                testCategory.getId(),
                "Future transaction",
                today.plusDays(5)
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(request), jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/transactions — list
    // ──────────────────────────────────────────────

    @Test
    void getTransactions_ShouldReturn200_WithPagination() {
        // Insert several transactions
        for (int i = 1; i <= 3; i++) {
            CreateTransactionRequest req = new CreateTransactionRequest(
                    BigDecimal.valueOf(i * 50),
                    TransactionType.EXPENSE,
                    testCategory.getId(),
                    "Transaction " + i,
                    today.minusDays(i)
            );
            ResponseEntity<String> createResponse = restTemplate.postForEntity(
                    url("/api/v1/transactions"), jsonEntityWithAuth(write(req), jwtToken), String.class);
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Fetch list
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions"), HttpMethod.GET, authEntity(jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<Object>read(body, "$.data")).isInstanceOf(java.util.List.class);
        assertThat(JsonPath.<Integer>read(body, "$.data.length()")).isEqualTo(3);
        assertThat(JsonPath.<Object>read(body, "$.pagination")).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.pagination.page")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(body, "$.pagination.limit")).isEqualTo(20);
        assertThat(JsonPath.<Integer>read(body, "$.pagination.total")).isEqualTo(3);
        assertThat(JsonPath.<Integer>read(body, "$.pagination.totalPages")).isEqualTo(1);
    }

    @Test
    void getTransactions_ShouldReturn200_WithPaginationParams() {
        // Insert 5 transactions
        for (int i = 1; i <= 5; i++) {
            CreateTransactionRequest req = new CreateTransactionRequest(
                    BigDecimal.valueOf(i * 10),
                    TransactionType.EXPENSE,
                    testCategory.getId(),
                    "Item " + i,
                    today.minusDays(i)
            );
            ResponseEntity<String> createResponse = restTemplate.postForEntity(
                    url("/api/v1/transactions"), jsonEntityWithAuth(write(req), jwtToken), String.class);
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Fetch page 2 with limit 2
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions?page={page}&limit={limit}"), HttpMethod.GET,
                authEntity(jwtToken), String.class, 2, 2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<Object>read(body, "$.data")).isInstanceOf(java.util.List.class);
        assertThat(JsonPath.<Integer>read(body, "$.data.length()")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(body, "$.pagination.page")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(body, "$.pagination.limit")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(body, "$.pagination.total")).isEqualTo(5);
        assertThat(JsonPath.<Integer>read(body, "$.pagination.totalPages")).isEqualTo(3);
    }

    @Test
    void getTransactions_ShouldReturn200_WhenEmpty() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions"), HttpMethod.GET, authEntity(jwtToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<Object>read(body, "$.data")).isInstanceOf(java.util.List.class);
        assertThat(JsonPath.<Integer>read(body, "$.data.length()")).isEqualTo(0);
        assertThat(JsonPath.<Integer>read(body, "$.pagination.total")).isEqualTo(0);
    }

    @Test
    void getTransactions_ShouldSupportFiltering() {
        // Create both income and expense
        CreateTransactionRequest income = new CreateTransactionRequest(
                BigDecimal.valueOf(1000), TransactionType.INCOME,
                testCategory.getId(), "Salary", today
        );
        restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(income), jwtToken), String.class);

        CreateTransactionRequest expense = new CreateTransactionRequest(
                BigDecimal.valueOf(50), TransactionType.EXPENSE,
                testCategory.getId(), "Coffee", today
        );
        restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(expense), jwtToken), String.class);

        // Filter by type
        ResponseEntity<String> typeResponse = restTemplate.exchange(
                url("/api/v1/transactions?type={type}"), HttpMethod.GET,
                authEntity(jwtToken), String.class, "INCOME");
        assertThat(typeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String typeBody = typeResponse.getBody();
        assertThat(typeBody).isNotNull();
        assertThat(JsonPath.<Integer>read(typeBody, "$.data.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(typeBody, "$.data[0].type")).isEqualTo("INCOME");

        // Filter by search
        ResponseEntity<String> searchResponse = restTemplate.exchange(
                url("/api/v1/transactions?search={search}"), HttpMethod.GET,
                authEntity(jwtToken), String.class, "Coffee");
        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String searchBody = searchResponse.getBody();
        assertThat(searchBody).isNotNull();
        assertThat(JsonPath.<Integer>read(searchBody, "$.data.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(searchBody, "$.data[0].description")).isEqualTo("Coffee");

        // Filter by date range
        ResponseEntity<String> dateResponse = restTemplate.exchange(
                url("/api/v1/transactions?from={from}&to={to}"), HttpMethod.GET,
                authEntity(jwtToken), String.class,
                today.minusDays(1).toString(), today.plusDays(1).toString());
        assertThat(dateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String dateBody = dateResponse.getBody();
        assertThat(dateBody).isNotNull();
        assertThat(JsonPath.<Integer>read(dateBody, "$.data.length()")).isEqualTo(2);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/transactions/{id}
    // ──────────────────────────────────────────────

    @Test
    void getTransactionById_ShouldReturn200_WhenExists() {
        // Create a transaction
        String createBody = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(75.00),
                                TransactionType.EXPENSE,
                                testCategory.getId(),
                                "Groceries",
                                today)), jwtToken), String.class).getBody();
        assertThat(createBody).isNotNull();
        String transactionId = JsonPath.read(createBody, "$.data.id");

        // Fetch by ID (with auth)
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.GET,
                authEntity(jwtToken), String.class, transactionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.id")).isEqualTo(transactionId);
        assertThat(JsonPath.<Object>read(body, "$.data.amount")).isNotNull();
        assertThat(JsonPath.<String>read(body, "$.data.description")).isEqualTo("Groceries");
        assertThat(JsonPath.<String>read(body, "$.data.date")).isEqualTo(today.toString());
        assertThat(JsonPath.<String>read(body, "$.data.userId")).isEqualTo(testUser.getId().toString());
    }

    @Test
    void getTransactionById_ShouldReturn404_WhenNotFound() {
        UUID fakeId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.GET,
                authEntity(jwtToken), String.class, fakeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(404);
        assertThat(JsonPath.<String>read(body, "$.error.message")).isEqualTo("Transaction not found or not accessible");
    }

    @Test
    void getTransactionById_ShouldReturn404_WhenNotOwned() {
        // Create another user with their own category
        User otherUser = User.builder()
                .email("other@example.com")
                .name("Other User")
                .passwordHash("irrelevant")
                .currency("USD")
                .emailVerified(true)
                .active(true)
                .build();
        otherUser = userRepository.save(otherUser);

        Category otherCategory = Category.builder()
                .name("Other Category")
                .user(otherUser)
                .isDefault(false)
                .build();
        otherCategory = categoryRepository.save(otherCategory);

        String otherToken = jwtProvider.generateAccessToken(otherUser.getId(), otherUser.getEmail());

        // Other user creates a transaction
        String createBody = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(200),
                                TransactionType.INCOME,
                                otherCategory.getId(),
                                "Other income",
                                today)), otherToken), String.class).getBody();
        assertThat(createBody).isNotNull();
        String otherTransactionId = JsonPath.read(createBody, "$.data.id");

        // Test user tries to access other user's transaction (with auth)
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.GET,
                authEntity(jwtToken), String.class, otherTransactionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<String>read(body, "$.error.message"))
                .isEqualTo("Transaction not found or not accessible");
    }

    // ──────────────────────────────────────────────
    // PATCH /api/v1/transactions/{id}
    // ──────────────────────────────────────────────

    @Test
    void updateTransaction_ShouldReturn200_WhenValid() {
        // Create
        String createBody = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(100.00),
                                TransactionType.INCOME,
                                testCategory.getId(),
                                "Salary",
                                today)), jwtToken), String.class).getBody();
        assertThat(createBody).isNotNull();
        String txId = JsonPath.read(createBody, "$.data.id");

        // Update
        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(
                BigDecimal.valueOf(150.00),
                TransactionType.INCOME,
                null,  // keep same category
                "Updated salary",
                today
        );

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.PATCH,
                jsonEntityWithAuth(write(updateReq), jwtToken), String.class, txId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.id")).isEqualTo(txId);
        assertThat(JsonPath.<Object>read(body, "$.data.amount")).isNotNull();
        assertThat(JsonPath.<String>read(body, "$.data.description")).isEqualTo("Updated salary");
    }

    @Test
    void updateTransaction_ShouldReturn200_WhenPartialUpdate() {
        // Create
        String createBody = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(200.00),
                                TransactionType.EXPENSE,
                                testCategory.getId(),
                                "Initial description",
                                today)), jwtToken), String.class).getBody();
        assertThat(createBody).isNotNull();
        String txId = JsonPath.read(createBody, "$.data.id");

        // Partial update — only change description
        String partialPatch = """
                {
                    "description": "Only description changed"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.PATCH,
                jsonEntityWithAuth(partialPatch, jwtToken), String.class, txId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<String>read(body, "$.data.description")).isEqualTo("Only description changed");
        assertThat(JsonPath.<Object>read(body, "$.data.amount")).isNotNull();  // unchanged
        assertThat(JsonPath.<String>read(body, "$.data.type")).isEqualTo("EXPENSE");  // unchanged
    }

    @Test
    void updateTransaction_ShouldReturn404_WhenNotFound() {
        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(
                BigDecimal.valueOf(99), TransactionType.EXPENSE,
                testCategory.getId(), "Nope", today
        );

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.PATCH,
                jsonEntityWithAuth(write(updateReq), jwtToken), String.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateTransaction_ShouldReturn422_WhenInvalidAmount() {
        // Create
        String createBody = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(50),
                                TransactionType.EXPENSE,
                                testCategory.getId(),
                                "Test",
                                today)), jwtToken), String.class).getBody();
        assertThat(createBody).isNotNull();
        String txId = JsonPath.read(createBody, "$.data.id");

        // Try to set a negative amount
        String invalidPatch = """
                {
                    "amount": -100
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.PATCH,
                jsonEntityWithAuth(invalidPatch, jwtToken), String.class, txId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    @Test
    void updateTransaction_ShouldReturn422_WhenInvalidDate() {
        // Create
        String createBody = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(50),
                                TransactionType.EXPENSE,
                                testCategory.getId(),
                                "Test",
                                today)), jwtToken), String.class).getBody();
        assertThat(createBody).isNotNull();
        String txId = JsonPath.read(createBody, "$.data.id");

        // Try to set a future date
        String invalidPatch = """
                {
                    "date": "%s"
                }
                """.formatted(today.plusDays(10));

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.PATCH,
                jsonEntityWithAuth(invalidPatch, jwtToken), String.class, txId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    // ──────────────────────────────────────────────
    // DELETE /api/v1/transactions/{id}
    // ──────────────────────────────────────────────

    @Test
    void deleteTransaction_ShouldReturn204_WhenExists() {
        // Create
        String createBody = restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(50.00),
                                TransactionType.EXPENSE,
                                testCategory.getId(),
                                "To be deleted",
                                today)), jwtToken), String.class).getBody();
        assertThat(createBody).isNotNull();
        String txId = JsonPath.read(createBody, "$.data.id");

        // Delete
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.DELETE,
                authEntity(jwtToken), String.class, txId);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify deletion (with auth)
        ResponseEntity<String> getResponse = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.GET,
                authEntity(jwtToken), String.class, txId);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteTransaction_ShouldReturn404_WhenNotFound() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/transactions/{id}"), HttpMethod.DELETE,
                authEntity(jwtToken), String.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(404);
    }

    // ──────────────────────────────────────────────
    // Transaction isolation between users
    // ──────────────────────────────────────────────

    @Test
    void transactions_ShouldBeIsolated_ByUser() {
        // Test user creates a transaction
        restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(100),
                                TransactionType.INCOME,
                                testCategory.getId(),
                                "User A income",
                                today)), jwtToken), String.class);

        // Second user with own category
        User userB = User.builder()
                .email("userb@example.com")
                .name("User B")
                .passwordHash("irrelevant")
                .currency("EUR")
                .emailVerified(true)
                .active(true)
                .build();
        userB = userRepository.save(userB);

        Category categoryB = Category.builder()
                .name("B's Category")
                .user(userB)
                .isDefault(false)
                .build();
        categoryB = categoryRepository.save(categoryB);

        String tokenB = jwtProvider.generateAccessToken(userB.getId(), userB.getEmail());

        // User B creates a transaction
        restTemplate.postForEntity(
                url("/api/v1/transactions"), jsonEntityWithAuth(write(
                        new CreateTransactionRequest(
                                BigDecimal.valueOf(200),
                                TransactionType.EXPENSE,
                                categoryB.getId(),
                                "User B expense",
                                today)), tokenB), String.class);

        // User A should only see their own transaction
        ResponseEntity<String> responseA = restTemplate.exchange(
                url("/api/v1/transactions"), HttpMethod.GET, authEntity(jwtToken), String.class);
        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);
        String bodyA = responseA.getBody();
        assertThat(bodyA).isNotNull();
        assertThat(JsonPath.<Integer>read(bodyA, "$.data.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(bodyA, "$.data[0].description")).isEqualTo("User A income");

        // User B should only see their own transaction
        ResponseEntity<String> responseB = restTemplate.exchange(
                url("/api/v1/transactions"), HttpMethod.GET, authEntity(tokenB), String.class);
        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.OK);
        String bodyB = responseB.getBody();
        assertThat(bodyB).isNotNull();
        assertThat(JsonPath.<Integer>read(bodyB, "$.data.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(bodyB, "$.data[0].description")).isEqualTo("User B expense");
    }

    // ──────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }
}
