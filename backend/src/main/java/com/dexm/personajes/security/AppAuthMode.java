package com.dexm.personajes.security;

public enum AppAuthMode {
    LOCAL,
    IAP;

    public static AppAuthMode from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception exception) {
            throw new IllegalStateException("APP_AUTH_MODE must be either 'local' or 'iap'");
        }
    }
}
