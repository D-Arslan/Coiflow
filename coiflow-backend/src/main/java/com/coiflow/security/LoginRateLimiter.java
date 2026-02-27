package com.coiflow.security;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 3;
    private static final int BLOCK_DURATION_MINUTES = 10;

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        LoginAttempt attempt = attempts.get(email);
        if (attempt == null) return false;
        if (attempt.blockedUntil != null && LocalDateTime.now().isBefore(attempt.blockedUntil)) {
            return true;
        }
        if (attempt.blockedUntil != null && LocalDateTime.now().isAfter(attempt.blockedUntil)) {
            attempts.remove(email);
            return false;
        }
        return false;
    }

    public void recordFailure(String email) {
        LoginAttempt attempt = attempts.computeIfAbsent(email, k -> new LoginAttempt());
        attempt.failCount++;
        if (attempt.failCount >= MAX_ATTEMPTS) {
            attempt.blockedUntil = LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES);
        }
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    private static class LoginAttempt {
        int failCount = 0;
        LocalDateTime blockedUntil;
    }
}
