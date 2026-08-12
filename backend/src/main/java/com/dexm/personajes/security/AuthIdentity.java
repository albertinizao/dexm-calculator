package com.dexm.personajes.security;

import org.springframework.security.core.Authentication;
import java.util.Locale;

public record AuthIdentity(String subject, String email, String name) {
    public static AuthIdentity from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Object principal = authentication.getPrincipal();
        String email = null, name = null, subject = authentication.getName();
        if (principal instanceof AuthenticatedPrincipal user) {
            email = user.email(); name = user.name();
            if (user.subject() != null && !user.subject().isBlank()) subject = user.subject();
        }
        if (subject == null || subject.isBlank() || email == null || email.isBlank())
            throw new org.springframework.security.access.AccessDeniedException("Authenticated subject and email required");
        return new AuthIdentity(subject, normalizeEmail(email), name == null ? email : name);
    }
    public static String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}
