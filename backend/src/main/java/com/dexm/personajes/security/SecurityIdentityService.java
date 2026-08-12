package com.dexm.personajes.security;

import com.dexm.personajes.adapter.out.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SecurityIdentityService {
    private final UserRepository users;
    private final Set<String> adminEmails;
    private final AppAuthProperties auth;
    public SecurityIdentityService(UserRepository users, org.springframework.core.env.Environment env, AppAuthProperties auth) {
        this.users = users;
        this.auth = auth;
        this.adminEmails = Arrays.stream(Optional.ofNullable(env.getProperty("app.security.admin-emails", "")).orElse("").split(","))
                .map(AuthIdentity::normalizeEmail).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public AuthIdentity current(Authentication authentication) { return AuthIdentity.from(authentication); }
    public UserEntity provision(Authentication authentication) {
        AuthIdentity identity = current(authentication);
        return users.findByGoogleSubject(identity.subject()).map(existing -> { existing.update(identity.email(), identity.name()); return users.save(existing); })
                .orElseGet(() -> users.save(new UserEntity(UUID.randomUUID().toString(), identity.subject(), identity.email(), identity.name())));
    }
    public boolean isAdmin(AuthIdentity identity) { return auth.mode() == AppAuthMode.LOCAL || adminEmails.contains(identity.email()); }
    public UserEntity requireCurrentUser(Authentication authentication) { return users.findByGoogleSubject(current(authentication).subject()).orElseGet(() -> provision(authentication)); }
    public Set<String> adminEmails() { return adminEmails; }
    public String authMode() { return auth.mode().name().toLowerCase(java.util.Locale.ROOT); }
}
