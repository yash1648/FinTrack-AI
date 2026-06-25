package com.grim.backend.auth.service;

import com.grim.backend.auth.dto.*;
import com.grim.backend.auth.entity.PasswordResetToken;
import com.grim.backend.auth.entity.RefreshToken;
import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.repository.PasswordResetTokenRepository;
import com.grim.backend.auth.repository.RefreshTokenRepository;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.auth.security.JwtProvider;
import com.grim.backend.common.exception.AccountLockedException;
import com.grim.backend.common.exception.ConflictException;
import com.grim.backend.common.exception.EmailNotVerifiedException;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

    // ──────────────────────────────────────────────────────────────
    // Dependencies (all 9 mocked)
    // ──────────────────────────────────────────────────────────────

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private PasswordBlocklistService passwordBlocklistService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    // ──────────────────────────────────────────────────────────────
    // Shared test data
    // ──────────────────────────────────────────────────────────────

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "Strong@Pass1";
    private static final String NAME = "Test User";
    private static final String CURRENCY = "USD";
    private static final String ACCESS_TOKEN = "access-jwt-token";

    // ──────────────────────────────────────────────────────────────
    // registerUser
    // ──────────────────────────────────────────────────────────────

    @Test
    void registerUser_Success() {
        // Given
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, NAME, CURRENCY);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordBlocklistService.isBlocked(PASSWORD)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        // When
        authService.registerUser(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getName()).isEqualTo(NAME);
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getCurrency()).isEqualTo(CURRENCY);
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getVerificationToken()).isNotNull();
        assertThat(saved.getVerificationTokenExpiry()).isNotNull();

        verify(emailService).sendVerificationEmail(eq(EMAIL), anyString());
    }

    @Test
    void registerUser_NullCurrency_DefaultsToINR() {
        // Given
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, NAME, null);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordBlocklistService.isBlocked(PASSWORD)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        // When
        authService.registerUser(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getCurrency()).isEqualTo("INR");

        verify(emailService).sendVerificationEmail(eq(EMAIL), anyString());
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsConflictException() {
        // Given
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, NAME, CURRENCY);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void registerUser_BlockedPassword_ThrowsIllegalArgumentException() {
        // Given
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, NAME, CURRENCY);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordBlocklistService.isBlocked(PASSWORD)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too common");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    // ──────────────────────────────────────────────────────────────
    // verifyEmail
    // ──────────────────────────────────────────────────────────────

    @Test
    void verifyEmail_Success() {
        // Given
        String token = "valid-token-123";
        User user = mock(User.class);

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));
        when(user.isEmailVerified()).thenReturn(false);
        when(user.getVerificationTokenExpiry()).thenReturn(LocalDateTime.now().plusHours(1));

        // When
        authService.verifyEmail(token);

        // Then
        verify(user).setEmailVerified(true);
        verify(user).setVerificationToken(null);
        verify(user).setVerificationTokenExpiry(null);
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_InvalidToken_ThrowsIllegalArgumentException() {
        // Given
        String token = "non-existent-token";
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.verifyEmail(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired verification token");

        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_AlreadyVerified_ThrowsIllegalArgumentException() {
        // Given
        String token = "already-verified-token";
        User user = mock(User.class);

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));
        when(user.isEmailVerified()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.verifyEmail(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already verified");

        verify(user, never()).setEmailVerified(anyBoolean());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_ExpiredToken_ThrowsIllegalArgumentException() {
        // Given
        String token = "expired-token";
        User user = mock(User.class);

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));
        when(user.isEmailVerified()).thenReturn(false);
        when(user.getVerificationTokenExpiry()).thenReturn(LocalDateTime.now().minusMinutes(5));

        // When & Then
        assertThatThrownBy(() -> authService.verifyEmail(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        verify(user, never()).setEmailVerified(anyBoolean());
        verify(userRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────
    // loginUser
    // ──────────────────────────────────────────────────────────────

    @Test
    void loginUser_Success() {
        // Given
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        User user = mock(User.class);

        when(loginAttemptService.isLocked(EMAIL)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.isEmailVerified()).thenReturn(true);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getName()).thenReturn(NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getCurrency()).thenReturn(CURRENCY);
        when(jwtProvider.generateAccessToken(USER_ID, EMAIL)).thenReturn(ACCESS_TOKEN);

        // When
        AuthResponse response = authService.loginUser(request);

        // Then
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.user()).isNotNull();
        assertThat(response.user().email()).isEqualTo(EMAIL);
        assertThat(response.user().name()).isEqualTo(NAME);
        assertThat(response.user().currency()).isEqualTo(CURRENCY);
        assertThat(response.user().id()).isEqualTo(USER_ID);

        verify(loginAttemptService).loginSucceeded(EMAIL);
        verify(loginAttemptService, never()).loginFailed(anyString());

        ArgumentCaptor<RefreshToken> rtCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(rtCaptor.capture());
        RefreshToken saved = rtCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTokenHash()).isNotNull();
        assertThat(saved.getExpiresAt()).isNotNull();
    }

    @Test
    void loginUser_LockedAccount_ThrowsAccountLockedException() {
        // Given
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        when(loginAttemptService.isLocked(EMAIL)).thenReturn(true);
        when(loginAttemptService.getRemainingLockTime(EMAIL)).thenReturn(15L);

        // When & Then
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("locked");

        verify(authenticationManager, never()).authenticate(any());
        verify(userRepository, never()).findByEmail(anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginUser_UnverifiedEmail_ThrowsEmailNotVerifiedException() {
        // Given
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        User user = mock(User.class);

        when(loginAttemptService.isLocked(EMAIL)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.isEmailVerified()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("Email not verified");

        verify(loginAttemptService).loginSucceeded(EMAIL);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginUser_BadCredentials_RethrowsAuthenticationException() {
        // Given
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        when(loginAttemptService.isLocked(EMAIL)).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).loginFailed(EMAIL);
        verify(loginAttemptService, never()).loginSucceeded(anyString());
        verify(userRepository, never()).findByEmail(anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────
    // refreshToken (rotation, expired, invalid, null, empty)
    // ──────────────────────────────────────────────────────────────

    @Test
    void refreshToken_Success() {
        // Given
        String oldRefreshTokenValue = "old-refresh-token-abc";
        String tokenHash = DigestUtils.sha256Hex(oldRefreshTokenValue);

        RefreshToken existingToken = mock(RefreshToken.class);
        User user = mock(User.class);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(existingToken));
        when(existingToken.getExpiresAt()).thenReturn(LocalDateTime.now().plusDays(7));
        when(existingToken.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getEmail()).thenReturn(EMAIL);
        when(jwtProvider.generateAccessToken(USER_ID, EMAIL)).thenReturn("new-access-token");

        // When
        AccessTokenResponse response = authService.refreshToken(oldRefreshTokenValue);

        // Then
        assertThat(response.accessToken()).isEqualTo("new-access-token");

        verify(refreshTokenRepository).delete(existingToken);
        verify(refreshTokenRepository).flush();

        ArgumentCaptor<RefreshToken> newTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(newTokenCaptor.capture());
        RefreshToken newToken = newTokenCaptor.getValue();
        assertThat(newToken.getUser()).isEqualTo(user);
        assertThat(newToken.getTokenHash()).isNotNull();
        assertThat(newToken.getExpiresAt()).isNotNull();
    }

    @Test
    void refreshToken_ExpiredToken_ThrowsIllegalArgumentException() {
        // Given
        String expiredTokenValue = "expired-refresh-token";
        String tokenHash = DigestUtils.sha256Hex(expiredTokenValue);

        RefreshToken existingToken = mock(RefreshToken.class);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(existingToken));
        when(existingToken.getExpiresAt()).thenReturn(LocalDateTime.now().minusMinutes(1));

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(expiredTokenValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(existingToken);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).flush();
    }

    @Test
    void refreshToken_InvalidToken_ThrowsIllegalArgumentException() {
        // Given
        String invalidToken = "non-existent-token";
        String tokenHash = DigestUtils.sha256Hex(invalidToken);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository, never()).flush();
    }

    @Test
    void refreshToken_NullToken_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> authService.refreshToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token is required");

        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(anyString());
    }

    @Test
    void refreshToken_EmptyToken_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> authService.refreshToken(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token is required");

        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(anyString());
    }

    // ──────────────────────────────────────────────────────────────
    // forgotPassword
    // ──────────────────────────────────────────────────────────────

    @Test
    void forgotPassword_ExistingUser_SendsEmail() {
        // Given
        User user = new User();
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // When
        authService.forgotPassword(EMAIL);

        // Then
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.getTokenHash()).isNotNull();
        assertThat(savedToken.getExpiresAt()).isNotNull();

        verify(emailService).sendPasswordResetEmail(eq(EMAIL), anyString());
    }

    @Test
    void forgotPassword_NonExistingUser_DoesNothing() {
        // Given
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // When
        authService.forgotPassword(EMAIL);

        // Then
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    // ──────────────────────────────────────────────────────────────
    // resetPassword
    // ──────────────────────────────────────────────────────────────

    @Test
    void resetPassword_Success() {
        // Given
        String resetToken = "valid-reset-token-abc";
        String newPassword = "NewPass@123";
        String tokenHash = DigestUtils.sha256Hex(resetToken);

        User user = mock(User.class);
        PasswordResetToken passwordResetToken = mock(PasswordResetToken.class);

        when(passwordBlocklistService.isBlocked(newPassword)).thenReturn(false);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(passwordResetToken));
        when(passwordResetToken.isUsed()).thenReturn(false);
        when(passwordResetToken.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(1));
        when(passwordResetToken.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(passwordEncoder.encode(newPassword)).thenReturn("hashed-new-password");

        // When
        authService.resetPassword(resetToken, newPassword);

        // Then
        verify(user).setPasswordHash("hashed-new-password");
        verify(userRepository).save(user);
        verify(passwordResetToken).setUsed(true);
        verify(passwordResetTokenRepository).save(passwordResetToken);
        verify(refreshTokenRepository).deleteByUserId(USER_ID);
    }

    @Test
    void resetPassword_BlockedPassword_ThrowsIllegalArgumentException() {
        // Given
        when(passwordBlocklistService.isBlocked(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword("any-token", "blocked-pass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too common");

        verify(passwordResetTokenRepository, never()).findByTokenHash(anyString());
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void resetPassword_InvalidToken_ThrowsIllegalArgumentException() {
        // Given
        String resetToken = "non-existent-token";
        String newPassword = "NewPass@123";
        String tokenHash = DigestUtils.sha256Hex(resetToken);

        when(passwordBlocklistService.isBlocked(newPassword)).thenReturn(false);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(resetToken, newPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void resetPassword_UsedToken_ThrowsIllegalArgumentException() {
        // Given
        String resetToken = "used-reset-token";
        String newPassword = "NewPass@456";
        String tokenHash = DigestUtils.sha256Hex(resetToken);

        PasswordResetToken passwordResetToken = mock(PasswordResetToken.class);

        when(passwordBlocklistService.isBlocked(newPassword)).thenReturn(false);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(passwordResetToken));
        when(passwordResetToken.isUsed()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(resetToken, newPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsIllegalArgumentException() {
        // Given
        String resetToken = "expired-reset-token";
        String newPassword = "NewPass@789";
        String tokenHash = DigestUtils.sha256Hex(resetToken);

        PasswordResetToken passwordResetToken = mock(PasswordResetToken.class);

        when(passwordBlocklistService.isBlocked(newPassword)).thenReturn(false);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(passwordResetToken));
        when(passwordResetToken.isUsed()).thenReturn(false);
        when(passwordResetToken.getExpiresAt()).thenReturn(LocalDateTime.now().minusHours(1));

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(resetToken, newPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    // ──────────────────────────────────────────────────────────────
    // getProfile
    // ──────────────────────────────────────────────────────────────

    @Test
    void getProfile_Success() {
        // Given
        User user = mock(User.class);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(USER_ID);
        when(user.getName()).thenReturn(NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getCurrency()).thenReturn(CURRENCY);

        // When
        UserDto dto = authService.getProfile(EMAIL);

        // Then
        assertThat(dto.id()).isEqualTo(USER_ID);
        assertThat(dto.name()).isEqualTo(NAME);
        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.currency()).isEqualTo(CURRENCY);
    }

    @Test
    void getProfile_UserNotFound_ThrowsUsernameNotFoundException() {
        // Given
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.getProfile(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ──────────────────────────────────────────────────────────────
    // updateProfile
    // ──────────────────────────────────────────────────────────────

    @Test
    void updateProfile_Success() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "EUR");
        User user = mock(User.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(USER_ID);
        when(user.getName()).thenReturn("Updated Name");
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getCurrency()).thenReturn("EUR");

        // When
        UserDto result = authService.updateProfile(EMAIL, request);

        // Then
        verify(user).setName("Updated Name");
        verify(user).setCurrency("EUR");
        verify(userRepository).save(user);

        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.id()).isEqualTo(USER_ID);
    }

    @Test
    void updateProfile_PartialUpdate_Success() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest("New Name Only", null);
        User user = mock(User.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(USER_ID);
        when(user.getName()).thenReturn("New Name Only");
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getCurrency()).thenReturn("INR");

        // When
        UserDto result = authService.updateProfile(EMAIL, request);

        // Then
        verify(user).setName("New Name Only");
        verify(user, never()).setCurrency(anyString());
        verify(userRepository).save(user);

        assertThat(result.name()).isEqualTo("New Name Only");
        assertThat(result.currency()).isEqualTo("INR");
    }

    @Test
    void updateProfile_UserNotFound_ThrowsUsernameNotFoundException() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest("Name", "USD");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.updateProfile(EMAIL, request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────
    // changePassword
    // ──────────────────────────────────────────────────────────────

    @Test
    void changePassword_Success() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@456");
        User user = mock(User.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("encoded-old-password");
        when(passwordEncoder.matches("OldPass@123", "encoded-old-password")).thenReturn(true);
        when(passwordBlocklistService.isBlocked("NewPass@456")).thenReturn(false);
        when(user.getId()).thenReturn(USER_ID);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("encoded-new-password");

        // When
        authService.changePassword(EMAIL, request);

        // Then
        verify(user).setPasswordHash("encoded-new-password");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).deleteByUserId(USER_ID);
    }

    @Test
    void changePassword_UserNotFound_ThrowsUsernameNotFoundException() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@456");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsIllegalArgumentException() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest("WrongOld@Pass", "NewPass@456");
        User user = mock(User.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("encoded-old-password");
        when(passwordEncoder.matches("WrongOld@Pass", "encoded-old-password")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incorrect current password");

        verify(user, never()).setPasswordHash(anyString());
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void changePassword_BlockedNewPassword_ThrowsIllegalArgumentException() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "Common@Pass");
        User user = mock(User.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("encoded-old-password");
        when(passwordEncoder.matches("OldPass@123", "encoded-old-password")).thenReturn(true);
        when(passwordBlocklistService.isBlocked("Common@Pass")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too common");

        verify(user, never()).setPasswordHash(anyString());
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    // ──────────────────────────────────────────────────────────────
    // logout
    // ──────────────────────────────────────────────────────────────

    @Test
    void logout_Success() {
        // Given
        String refreshTokenValue = "some-refresh-token";
        String tokenHash = DigestUtils.sha256Hex(refreshTokenValue);

        // When
        authService.logout(refreshTokenValue);

        // Then
        verify(refreshTokenRepository).deleteByTokenHash(tokenHash);
    }

    @Test
    void logout_DifferentToken_CallsDeleteWithCorrectHash() {
        // Given
        String refreshTokenValue = "another-refresh-token-xyz";
        String expectedHash = DigestUtils.sha256Hex(refreshTokenValue);

        // When
        authService.logout(refreshTokenValue);

        // Then
        verify(refreshTokenRepository).deleteByTokenHash(expectedHash);
    }
}
