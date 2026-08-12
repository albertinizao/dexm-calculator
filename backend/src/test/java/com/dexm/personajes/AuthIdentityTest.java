package com.dexm.personajes;

import com.dexm.personajes.security.AuthenticatedPrincipal;
import com.dexm.personajes.security.AuthIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthIdentityTest {
    @Test void normalizesAuthenticatedPrincipalEmail() {
        var principal = new AuthenticatedPrincipal("iap-subject", " Admin@Example.COM ", "Admin");
        var identity = AuthIdentity.from(new TestingAuthenticationToken(principal, null, "ROLE_USER"));
        assertThat(identity.subject()).isEqualTo("iap-subject");
        assertThat(identity.email()).isEqualTo("admin@example.com");
    }

    @Test void rejectsPrincipalWithoutEmail() {
        var principal = new AuthenticatedPrincipal("iap-subject", "", "Admin");
        assertThatThrownBy(() -> AuthIdentity.from(new TestingAuthenticationToken(principal, null, "ROLE_USER")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
