package com.dexm.personajes.security;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class IapAuthenticationFilter extends OncePerRequestFilter {
    static final String IAP_ASSERTION_HEADER = "X-Goog-IAP-JWT-Assertion";
    private final IapJwtVerifier verifier;

    IapAuthenticationFilter(IapJwtVerifier verifier) { this.verifier = verifier; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String assertion = request.getHeader(IAP_ASSERTION_HEADER);
        if (assertion == null || assertion.isBlank()) { reject(response); return; }
        try {
            IapJwtClaims claims = verifier.verify(assertion);
            var principal = new AuthenticatedPrincipal(claims.subject(), claims.email(), claims.name());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "N/A",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            SecurityContextHolder.setContext(context);
            try { filterChain.doFilter(request, response); }
            finally { SecurityContextHolder.clearContext(); }
        } catch (IapJwtVerificationException exception) { reject(response); }
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "IAP");
        response.sendError(HttpStatus.UNAUTHORIZED.value());
    }
}
