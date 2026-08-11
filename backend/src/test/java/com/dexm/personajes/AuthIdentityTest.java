package com.dexm.personajes;

import com.dexm.personajes.security.AuthIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.authentication.TestingAuthenticationToken;
import java.util.Map;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class AuthIdentityTest {
    @Test void normalizesVerifiedGoogleEmail() {
        var user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub","google-1","email"," Admin@Example.COM ","name","Admin","email_verified",true), "sub");
        var identity = AuthIdentity.from(new TestingAuthenticationToken(user, null, "ROLE_USER"));
        assertThat(identity.subject()).isEqualTo("google-1");
        assertThat(identity.email()).isEqualTo("admin@example.com");
    }
    @Test void rejectsUnverifiedEmail() {
        var user = new DefaultOAuth2User(List.of(), Map.of("sub","google-1","email","admin@example.com","email_verified",false), "sub");
        assertThatThrownBy(() -> AuthIdentity.from(new TestingAuthenticationToken(user, null, "ROLE_USER")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
