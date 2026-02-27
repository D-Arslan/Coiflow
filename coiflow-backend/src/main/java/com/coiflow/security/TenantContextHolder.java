package com.coiflow.security;

public final class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_SALON_ID = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static String getSalonId() {
        return CURRENT_SALON_ID.get();
    }

    public static void setSalonId(String salonId) {
        CURRENT_SALON_ID.set(salonId);
    }

    public static void clear() {
        CURRENT_SALON_ID.remove();
    }
}
