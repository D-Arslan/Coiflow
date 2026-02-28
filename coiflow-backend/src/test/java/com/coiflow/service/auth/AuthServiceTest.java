package com.coiflow.service.auth;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.auth.LoginRequest;
import com.coiflow.model.user.Manager;
import com.coiflow.model.user.RefreshToken;
import com.coiflow.repository.user.RefreshTokenRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import com.coiflow.security.LoginRateLimiter;
import com.coiflow.security.jwt.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.coiflow.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginRateLimiter rateLimiter;

    @InjectMocks private AuthService authService;

    private Manager user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 7L);
        user = aManager(SALON_ID);
        user.setEmail("karim@test.com");
        user.setPasswordHash("encoded");
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    // ── login ──────────────────────────────────────────────

    @Test
    void login_success() {
        when(rateLimiter.isBlocked("karim@test.com")).thenReturn(false);
        when(utilisateurRepository.findByEmail("karim@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), anyString()))
                .thenReturn("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthService.AuthResult result = authService.login(loginRequest("karim@test.com", "password123"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user().getEmail()).isEqualTo("karim@test.com");
        assertThat(result.user().getRole()).isEqualTo("MANAGER");
        verify(rateLimiter).recordSuccess("karim@test.com");
    }

    @Test
    void login_invalidEmail_throws() {
        when(rateLimiter.isBlocked("unknown@test.com")).thenReturn(false);
        when(utilisateurRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("unknown@test.com", "password")))
                .isInstanceOf(BadCredentialsException.class);
        verify(rateLimiter).recordFailure("unknown@test.com");
    }

    @Test
    void login_invalidPassword_throws() {
        when(rateLimiter.isBlocked("karim@test.com")).thenReturn(false);
        when(utilisateurRepository.findByEmail("karim@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("karim@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(rateLimiter).recordFailure("karim@test.com");
    }

    @Test
    void login_inactiveAccount_throws() {
        user.setActive(false);
        when(rateLimiter.isBlocked("karim@test.com")).thenReturn(false);
        when(utilisateurRepository.findByEmail("karim@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest("karim@test.com", "password123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void login_rateLimited_throws() {
        when(rateLimiter.isBlocked("karim@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest("karim@test.com", "password")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Trop de tentatives");
    }

    @Test
    void login_emailCaseInsensitive() {
        when(rateLimiter.isBlocked("karim@test.com")).thenReturn(false);
        when(utilisateurRepository.findByEmail("karim@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), anyString()))
                .thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Email with uppercase + spaces → should be normalized
        AuthService.AuthResult result = authService.login(loginRequest("  KARIM@TEST.COM  ", "password123"));

        assertThat(result.user().getEmail()).isEqualTo("karim@test.com");
    }

    // ── refresh ────────────────────────────────────────────

    @Test
    void refresh_success() {
        RefreshToken stored = RefreshToken.builder()
                .id("rt-001")
                .user(user)
                .token("old-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("new-access");
        when(jwtService.generateRefreshToken(anyString(), anyString()))
                .thenReturn("new-refresh");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthService.AuthResult result = authService.refresh("old-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        // Token was rotated
        assertThat(stored.getToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_expired_throws() {
        RefreshToken expired = RefreshToken.builder()
                .id("rt-002")
                .user(user)
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository).delete(expired);
    }
}
