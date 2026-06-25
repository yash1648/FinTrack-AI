package com.grim.backend.auth.controller;

import com.grim.backend.auth.dto.*;
import com.grim.backend.auth.entity.PasswordResetToken;
import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.repository.PasswordResetTokenRepository;
import com.grim.backend.auth.repository.RefreshTokenRepository;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.auth.security.JwtProvider;
import com.grim.backend.auth.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.codec.digest.DigestUtils;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

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
        // Wipe all data in dependency-safe order
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
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
    // POST /api/v1/auth/register
    // ──────────────────────────────────────────────

    @Test
    void register_ShouldReturn201_WhenValidRequest() {
        RegisterRequest request = new RegisterRequest(
                "fresh@example.com",
                "StrongP@ss1",
                "Fresh User",
                "USD"
        );

        HttpEntity<String> entity = jsonEntity(write(request));
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/register"), entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.message"))
                .isEqualTo("Registration successful. Please verify your email using the link sent to your email.");
    }

    @Test
    void register_ShouldReturn201_WithDefaultCurrency() {
        RegisterRequest request = new RegisterRequest(
                "default-currency@example.com",
                "StrongP@ss1",
                "No Currency",
                null
        );

        HttpEntity<String> entity = jsonEntity(write(request));
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/register"), entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
    }

    @Test
    void register_ShouldReturn409_WhenDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "dupe@example.com",
                "StrongP@ss1",
                "Dupe User",
                "EUR"
        );

        // First registration — should succeed
        HttpEntity<String> entity = jsonEntity(write(request));
        ResponseEntity<String> firstResponse = restTemplate.postForEntity(
                url("/api/v1/auth/register"), entity, String.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Duplicate email — should conflict
        ResponseEntity<String> secondResponse = restTemplate.postForEntity(
                url("/api/v1/auth/register"), entity, String.class);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        String body = secondResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(409);
        assertThat(JsonPath.<String>read(body, "$.error.message")).isEqualTo("Email already exists");
    }

    @Test
    void register_ShouldReturn422_WhenInvalidBody() {
        String invalidJson = """
                {
                    "email": "not-an-email",
                    "password": "short",
                    "name": ""
                }
                """;

        HttpEntity<String> entity = jsonEntity(invalidJson);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/register"), entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
        assertThat(JsonPath.<Object>read(body, "$.error.fields")).isInstanceOf(java.util.List.class);
    }

    @Test
    void register_ShouldReturn422_WhenMissingFields() {
        String json = """
                {
                    "email": "user@example.com"
                }
                """;

        HttpEntity<String> entity = jsonEntity(json);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/register"), entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/auth/login
    // ──────────────────────────────────────────────

    @Test
    void login_ShouldReturn200_WhenValidCredentials() {
        // Register
        RegisterRequest reg = new RegisterRequest(
                "logintest@example.com",
                "StrongP@ss1",
                "Login Test",
                "USD"
        );
        restTemplate.postForEntity(url("/api/v1/auth/register") ,jsonEntity(write(reg)), String.class);

        // Manually verify the user's email
        User user = userRepository.findByEmail("logintest@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found after registration"));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        // Login
        LoginRequest login = new LoginRequest("logintest@example.com", "StrongP@ss1");
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/login"), jsonEntity(write(login)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.accessToken")).isNotEmpty();
        assertThat(JsonPath.<String>read(body, "$.data.refreshToken")).isNotEmpty();
        assertThat(JsonPath.<String>read(body, "$.data.user.email")).isEqualTo("logintest@example.com");
        assertThat(JsonPath.<String>read(body, "$.data.user.name")).isEqualTo("Login Test");
    }

    @Test
    void login_ShouldReturn401_WhenInvalidCredentials() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "WrongP@ss1");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/login"), jsonEntity(write(request)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(401);
        assertThat(JsonPath.<String>read(body, "$.error.message")).isEqualTo("Invalid email or password");
    }

    @Test
    void login_ShouldReturn401_WhenWrongPassword() {
        // Register
        RegisterRequest reg = new RegisterRequest(
                "wrongpw@example.com",
                "StrongP@ss1",
                "Wrong PW",
                "USD"
        );
        restTemplate.postForEntity(url("/api/v1/auth/register") ,jsonEntity(write(reg)), String.class);

        // Try wrong password
        LoginRequest login = new LoginRequest("wrongpw@example.com", "WrongP@ss1");
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/login"), jsonEntity(write(login)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(401);
    }

    @Test
    void login_ShouldReturn403_WhenEmailNotVerified() {
        // Register (email stays unverified)
        RegisterRequest reg = new RegisterRequest(
                "unverified@example.com",
                "StrongP@ss1",
                "Unverified",
                "USD"
        );
        restTemplate.postForEntity(url("/api/v1/auth/register") ,jsonEntity(write(reg)), String.class);

        // Login without verifying email
        LoginRequest login = new LoginRequest("unverified@example.com", "StrongP@ss1");
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/login"), jsonEntity(write(login)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(403);
        assertThat(JsonPath.<String>read(body, "$.error.message"))
                .isEqualTo("Email not verified. Please verify your email before logging in.");
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/auth/refresh
    // ──────────────────────────────────────────────

    @Test
    void refresh_ShouldReturn200_WhenValidToken() {
        // Register + verify email + login to get a real refresh token
        RegisterRequest reg = new RegisterRequest(
                "refresh@example.com",
                "StrongP@ss1",
                "Refresh Test",
                "USD"
        );
        restTemplate.postForEntity(url("/api/v1/auth/register") ,jsonEntity(write(reg)), String.class);

        User user = userRepository.findByEmail("refresh@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                jsonEntity(write(new LoginRequest("refresh@example.com", "StrongP@ss1"))),
                String.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String loginBody = loginResponse.getBody();
        assertThat(loginBody).isNotNull();
        String refreshToken = JsonPath.read(loginBody, "$.data.refreshToken");

        // Now use the refresh token
        RefreshTokenRequest req = new RefreshTokenRequest(refreshToken);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"), jsonEntity(write(req)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.accessToken")).isNotEmpty();
    }

    @Test
    void refresh_ShouldReturn400_WhenInvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("garbage-refresh-token");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"), jsonEntity(write(request)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(400);
        assertThat(JsonPath.<String>read(body, "$.error.message")).isEqualTo("Invalid refresh token");
    }

    @Test
    void refresh_ShouldReturn400_WhenEmptyToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"), jsonEntity(write(request)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(400);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/auth/verify-email
    // ──────────────────────────────────────────────

    @Test
    void verifyEmail_ShouldReturn200_WhenValidToken() {
        // Register triggers creation of a verification token
        RegisterRequest reg = new RegisterRequest(
                "verify@example.com",
                "StrongP@ss1",
                "Verify User",
                "USD"
        );
        restTemplate.postForEntity(url("/api/v1/auth/register") ,jsonEntity(write(reg)), String.class);

        // Retrieve the token the system would have sent via email
        User user = userRepository.findByEmail("verify@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));
        String token = user.getVerificationToken();

        // Verify email
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/v1/auth/verify-email?token={token}"), String.class, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.message")).isEqualTo("Email verified successfully.");
    }

    @Test
    void verifyEmail_ShouldReturn400_WhenInvalidToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/v1/auth/verify-email?token={token}"), String.class, "nonexistent-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(400);
        assertThat(JsonPath.<String>read(body, "$.error.message"))
                .isEqualTo("Invalid or expired verification token");
    }

    @Test
    void verifyEmail_ShouldReturn400_WhenTokenAlreadyUsed() {
        // Register and verify
        RegisterRequest reg = new RegisterRequest(
                "verifyuser@example.com",
                "StrongP@ss1",
                "User",
                "USD"
        );
        restTemplate.postForEntity(url("/api/v1/auth/register") ,jsonEntity(write(reg)), String.class);

        User user = userRepository.findByEmail("verifyuser@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        // First verification
        ResponseEntity<String> firstResponse = restTemplate.getForEntity(
                url("/api/v1/auth/verify-email?token={token}"), String.class, user.getVerificationToken());
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // The token is single-use, so a second attempt should fail
        ResponseEntity<String> secondResponse = restTemplate.getForEntity(
                url("/api/v1/auth/verify-email?token={token}"), String.class, user.getVerificationToken());

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = secondResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<String>read(body, "$.error.message")).isEqualTo("Invalid or expired verification token");
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/auth/forgot-password
    // ──────────────────────────────────────────────

    @Test
    void forgotPassword_ShouldReturn200_ForAnyEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("anyone@example.com");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/forgot-password"), jsonEntity(write(request)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.message"))
                .isEqualTo("If your email is registered, a reset link has been sent.");
    }

    @Test
    void forgotPassword_ShouldReturn200_ForRegisteredEmail() {
        // Register a user first
        RegisterRequest reg = new RegisterRequest(
                "resetpw@example.com",
                "StrongP@ss1",
                "Reset PW",
                "USD"
        );
        restTemplate.postForEntity(url("/api/v1/auth/register") ,jsonEntity(write(reg)), String.class);

        // Request password reset for the registered email
        ForgotPasswordRequest request = new ForgotPasswordRequest("resetpw@example.com");
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/forgot-password"), jsonEntity(write(request)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
    }

    @Test
    void forgotPassword_ShouldReturn422_WhenInvalidEmail() {
        String invalidJson = """
                {
                    "email": "not-an-email"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/forgot-password"), jsonEntity(invalidJson), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/auth/profile
    // ──────────────────────────────────────────────

    @Test
    void getProfile_ShouldReturn200_WhenAuthenticated() {
        // Persist a user directly
        User user = User.builder()
                .email("profile@example.com")
                .name("Profile Test")
                .passwordHash("does-not-matter-for-jwt")
                .currency("EUR")
                .emailVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/profile"), HttpMethod.GET, authEntity(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.email")).isEqualTo("profile@example.com");
        assertThat(JsonPath.<String>read(body, "$.data.name")).isEqualTo("Profile Test");
    }

    @Test
    void getProfile_ShouldReturn401_WhenNoToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/v1/auth/profile"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(401);
        assertThat(JsonPath.<String>read(body, "$.error.message")).isEqualTo("Authentication required");
    }

    @Test
    void getProfile_ShouldReturn401_WhenMalformedToken() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/profile"), HttpMethod.GET, authEntity("obviously-invalid"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(401);
    }

    // ──────────────────────────────────────────────
    // PATCH /api/v1/auth/profile
    // ──────────────────────────────────────────────

    @Test
    void updateProfile_ShouldReturn200_WhenValid() {
        // Persist a user
        User user = User.builder()
                .email("update-profile@example.com")
                .name("Original Name")
                .passwordHash("irrelevant-for-jwt")
                .currency("USD")
                .emailVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());

        String patchBody = """
                {
                    "name": "Updated Name",
                    "currency": "EUR"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/profile"), HttpMethod.PATCH,
                jsonEntityWithAuth(patchBody, token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.name")).isEqualTo("Updated Name");
        assertThat(JsonPath.<String>read(body, "$.data.currency")).isEqualTo("EUR");
        assertThat(JsonPath.<String>read(body, "$.data.email")).isEqualTo("update-profile@example.com");
    }

    @Test
    void updateProfile_ShouldReturn200_WhenPartialUpdate() {
        // Only update name, keep existing currency
        User user = User.builder()
                .email("partial-update@example.com")
                .name("Partial")
                .passwordHash("irrelevant-for-jwt")
                .currency("GBP")
                .emailVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());

        String patchBody = """
                {
                    "name": "Partially Updated"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/profile"), HttpMethod.PATCH,
                jsonEntityWithAuth(patchBody, token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<String>read(body, "$.data.name")).isEqualTo("Partially Updated");
        assertThat(JsonPath.<String>read(body, "$.data.currency")).isEqualTo("GBP");
    }

    @Test
    void updateProfile_ShouldReturn401_WhenNoToken() {
        String patchBody = """
                { "name": "Hacker" }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/profile"), HttpMethod.PATCH,
                jsonEntity(patchBody), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(401);
    }

    // ──────────────────────────────────────────────
    // PATCH /api/v1/auth/change-password
    // ──────────────────────────────────────────────

    @Test
    void changePassword_ShouldReturn200_WhenValid() {
        // Create a user with a known password
        String rawPassword = "CurrentP@ss1";
        User user = User.builder()
                .email("changepw@example.com")
                .name("Change PW")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .currency("USD")
                .emailVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());

        String requestBody = """
                {
                    "currentPassword": "CurrentP@ss1",
                    "newPassword": "NewP@ssword1"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/change-password"), HttpMethod.PATCH,
                jsonEntityWithAuth(requestBody, token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.message")).isEqualTo("Password changed successfully.");
    }

    @Test
    void changePassword_ShouldReturn400_WhenWrongCurrentPassword() {
        String rawPassword = "CurrentP@ss1";
        User user = User.builder()
                .email("wrong-current@example.com")
                .name("Wrong Current")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .currency("USD")
                .emailVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());

        String requestBody = """
                {
                    "currentPassword": "WrongP@ss1",
                    "newPassword": "NewP@ssword1"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/change-password"), HttpMethod.PATCH,
                jsonEntityWithAuth(requestBody, token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(400);
    }

    @Test
    void changePassword_ShouldReturn422_WhenInvalidNewPassword() {
        String rawPassword = "CurrentP@ss1";
        User user = User.builder()
                .email("invalid-newpw@example.com")
                .name("Invalid New PW")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .currency("USD")
                .emailVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());

        // New password is too short and lacks required characters
        String requestBody = """
                {
                    "currentPassword": "CurrentP@ss1",
                    "newPassword": "short"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/change-password"), HttpMethod.PATCH,
                jsonEntityWithAuth(requestBody, token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/auth/reset-password
    // ──────────────────────────────────────────────

    @Test
    void resetPassword_ShouldReturn200_WhenValidToken() {
        // Create a user and a password reset token with a known value
        User user = User.builder()
                .email("reset-valid@example.com")
                .name("Reset Valid")
                .passwordHash(passwordEncoder.encode("OldP@ss1"))
                .currency("USD")
                .emailVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);

        // Create a reset token with a known raw value and its hash
        String rawToken = "valid-reset-token-123";
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        String requestBody = """
                {
                    "token": "valid-reset-token-123",
                    "newPassword": "NewResetP@ss1"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/reset-password"), jsonEntity(requestBody), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.message")).isEqualTo("Password reset successfully.");
    }

    @Test
    void resetPassword_ShouldReturn400_WhenInvalidToken() {
        String requestBody = """
                {
                    "token": "nonexistent-token",
                    "newPassword": "NewP@ssword1"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/reset-password"), jsonEntity(requestBody), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(400);
        assertThat(JsonPath.<String>read(body, "$.error.message")).isEqualTo("Invalid or expired reset token");
    }

    @Test
    void resetPassword_ShouldReturn400_WhenTokenExpired() {
        User user = User.builder()
                .email("reset-expired@example.com")
                .name("Reset Expired")
                .passwordHash(passwordEncoder.encode("OldP@ss1"))
                .currency("USD")
                .emailVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);

        String rawToken = "expired-reset-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().minusMinutes(5))  // Already expired
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        String requestBody = """
                {
                    "token": "expired-reset-token",
                    "newPassword": "NewP@ssword1"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/reset-password"), jsonEntity(requestBody), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(400);
    }

    @Test
    void resetPassword_ShouldReturn422_WhenValidationFails() {
        String requestBody = """
                {
                    "token": "some-token",
                    "newPassword": "short"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/reset-password"), jsonEntity(requestBody), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Integer>read(body, "$.error.code")).isEqualTo(422);
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/auth/logout
    // ──────────────────────────────────────────────

    @Test
    void logout_ShouldReturn200_WhenAuthenticated() {
        // Register + verify + login to get tokens
        RegisterRequest reg = new RegisterRequest(
                "logout@example.com",
                "StrongP@ss1",
                "Logout Test",
                "USD"
        );
        restTemplate.postForEntity(url("/api/v1/auth/register") ,jsonEntity(write(reg)), String.class);

        User user = userRepository.findByEmail("logout@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                jsonEntity(write(new LoginRequest("logout@example.com", "StrongP@ss1"))),
                String.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String loginBody = loginResponse.getBody();
        assertThat(loginBody).isNotNull();
        String refreshToken = JsonPath.read(loginBody, "$.data.refreshToken");

        // Logout
        RefreshTokenRequest logoutReq = new RefreshTokenRequest(refreshToken);
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/logout"), HttpMethod.POST,
                jsonEntityWithAuth(write(logoutReq), jwtProvider.generateAccessToken(user.getId(), user.getEmail())),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isTrue();
        assertThat(JsonPath.<String>read(body, "$.data.message")).isEqualTo("Logged out successfully");
    }

    @Test
    void logout_ShouldReturn401_WhenNoToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("some-token");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/logout"), jsonEntity(write(request)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
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
