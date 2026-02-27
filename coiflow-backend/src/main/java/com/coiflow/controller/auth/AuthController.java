package com.coiflow.controller.auth;

import com.coiflow.dto.auth.AuthResponse;
import com.coiflow.dto.auth.LoginRequest;
import com.coiflow.model.user.Utilisateur;
import com.coiflow.service.auth.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthService.AuthResult result = authService.login(request);

        addCookie(response, "access_token", result.accessToken(), 15 * 60);
        addCookie(response, "refresh_token", result.refreshToken(), 7 * 24 * 60 * 60);

        return ResponseEntity.ok(result.user());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        AuthService.AuthResult result = authService.refresh(refreshToken);

        addCookie(response, "access_token", result.accessToken(), 15 * 60);
        addCookie(response, "refresh_token", result.refreshToken(), 7 * 24 * 60 * 60);

        return ResponseEntity.ok(result.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Utilisateur user,
            HttpServletResponse response) {

        authService.logout(user.getId());

        deleteCookie(response, "access_token");
        deleteCookie(response, "refresh_token");

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal Utilisateur user) {
        if (user == null) {
            return ResponseEntity.noContent().build(); // 204 — no session, no console error
        }
        return ResponseEntity.ok(authService.me(user));
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
