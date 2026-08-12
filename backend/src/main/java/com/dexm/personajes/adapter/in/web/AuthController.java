package com.dexm.personajes.adapter.in.web;

import com.dexm.personajes.security.AuthorizationService;
import com.dexm.personajes.security.SecurityIdentityService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SecurityIdentityService identities;
    private final AuthorizationService authorization;

    public AuthController(SecurityIdentityService identities, AuthorizationService authorization) {
        this.identities = identities;
        this.authorization = authorization;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        var identity = identities.current(authentication);
        authorization.requireApplicationAccess(authentication);
        var user = identities.provision(authentication);
        return Map.of("id", user.getId(), "email", identity.email(), "name", identity.name(),
                "admin", identities.isAdmin(identity), "authMode", identities.authMode());
    }

    /** Kept for the SPA contract; neither local nor IAP owns an OAuth refresh token. */
    @PostMapping("/keepalive")
    public ResponseEntity<Void> keepalive(Authentication authentication) {
        authorization.requireApplicationAccess(authentication);
        return ResponseEntity.noContent().build();
    }

    /** IAP owns sign-out upstream and local mode has no session to invalidate. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() { return ResponseEntity.noContent().build(); }
}
