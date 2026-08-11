package com.dexm.personajes.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Locale;

public record AuthIdentity(String subject, String email, String name) {
    public static AuthIdentity from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Object principal = authentication.getPrincipal();
        String email = null, name = null, subject = authentication.getName();
        if (principal instanceof OAuth2User user) {
            email = user.getAttribute("email"); name = user.getAttribute("name");
            String claimSubject = user.getAttribute("sub"); if (claimSubject != null) subject = claimSubject;
            Object verified = user.getAttribute("email_verified");
            if (verified != null && !Boolean.parseBoolean(String.valueOf(verified))) throw new org.springframework.security.access.AccessDeniedException("Verified Google email required");
        }
        if (email == null || email.isBlank()) throw new org.springframework.security.access.AccessDeniedException("Verified Google email required");
        return new AuthIdentity(subject, normalizeEmail(email), name == null ? email : name);
    }
    public static String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}
