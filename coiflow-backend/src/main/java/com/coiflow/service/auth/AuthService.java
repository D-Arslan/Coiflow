package com.coiflow.service.auth;

import com.coiflow.dto.auth.AuthResponse;
import com.coiflow.dto.auth.LoginRequest;
import com.coiflow.model.user.RefreshToken;
import com.coiflow.model.user.Utilisateur;
import com.coiflow.repository.user.RefreshTokenRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import com.coiflow.security.LoginRateLimiter;
import com.coiflow.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;

    @Value("${jwt.refresh-token.expiration-days}")
    private long refreshTokenExpirationDays;

    @Transactional
    public AuthResult login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (rateLimiter.isBlocked(email)) {
            throw new IllegalStateException("Trop de tentatives. Réessayez dans quelques minutes.");
        }

        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> {
                    rateLimiter.recordFailure(email);
                    return new BadCredentialsException("Email ou mot de passe incorrect");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimiter.recordFailure(email);
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        if (!user.isActive()) {
            throw new IllegalStateException("Ce compte est désactivé");
        }

        rateLimiter.recordSuccess(email);

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getSalonId());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        // Persist refresh token
        refreshTokenRepository.deleteByUserId(user.getId());
        RefreshToken rt = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .build();
        refreshTokenRepository.save(rt);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .salonId(user.getSalonId())
                .build();

        return new AuthResult(accessToken, refreshToken, response);
    }

    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BadCredentialsException("Refresh token manquant");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalide"));

        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new BadCredentialsException("Refresh token expiré");
        }

        Utilisateur user = storedToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getSalonId());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        // Rotate refresh token
        storedToken.setToken(newRefreshToken);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays));
        refreshTokenRepository.save(storedToken);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .salonId(user.getSalonId())
                .build();

        return new AuthResult(newAccessToken, newRefreshToken, response);
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    public AuthResponse me(Utilisateur user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .salonId(user.getSalonId())
                .build();
    }

    public record AuthResult(String accessToken, String refreshToken, AuthResponse user) {}
}
