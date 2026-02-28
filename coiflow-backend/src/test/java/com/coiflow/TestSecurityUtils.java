package com.coiflow;

import com.coiflow.model.user.Utilisateur;
import com.coiflow.security.TenantContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helpers for setting up security & tenant context in unit tests.
 */
public final class TestSecurityUtils {

    private TestSecurityUtils() {}

    /**
     * Sets the TenantContextHolder to the given salonId.
     */
    public static void setTenantContext(String salonId) {
        TenantContextHolder.setSalonId(salonId);
    }

    /**
     * Clears both TenantContextHolder and SecurityContextHolder.
     * Call this in @AfterEach to avoid test contamination.
     */
    public static void clearAll() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    /**
     * Sets SecurityContextHolder's principal to the given Utilisateur.
     */
    public static void mockSecurityContext(Utilisateur user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
