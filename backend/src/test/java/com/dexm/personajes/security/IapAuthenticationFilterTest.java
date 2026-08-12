package com.dexm.personajes.security;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class IapAuthenticationFilterTest {
    @Test void rejectsRequestWithoutSignedAssertion() throws Exception {
        var response = new MockHttpServletResponse();
        new IapAuthenticationFilter(assertion -> new IapJwtClaims("sub", "email@example.com", "Name"))
                .doFilter(new MockHttpServletRequest(), response, (request, reply) -> { });
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void rejectsAssertionRejectedForWrongAudience() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(IapAuthenticationFilter.IAP_ASSERTION_HEADER, "bad-audience");
        var response = new MockHttpServletResponse();
        new IapAuthenticationFilter(assertion -> { throw new IapJwtVerificationException("Invalid IAP audience"); })
                .doFilter(request, response, (ignored, reply) -> { });
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void createsAuthenticatedIdentityFromVerifiedClaims() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(IapAuthenticationFilter.IAP_ASSERTION_HEADER, "verified");
        var response = new MockHttpServletResponse();
        var observed = new AtomicReference<AuthenticatedPrincipal>();
        new IapAuthenticationFilter(assertion -> new IapJwtClaims("iap-subject", "user@example.com", "User"))
                .doFilter(request, response, (ignored, reply) -> observed.set((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
        assertThat(observed.get()).isEqualTo(new AuthenticatedPrincipal("iap-subject", "user@example.com", "User"));
    }
}
