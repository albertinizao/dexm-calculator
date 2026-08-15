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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dexm.personajes.adapter.in.web.ErrorResponse;
import com.dexm.personajes.adapter.in.web.RequestCorrelationFilter;
import com.dexm.personajes.adapter.in.web.SafeLogException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;

final class IapAuthenticationFilter extends OncePerRequestFilter {
    static final String IAP_ASSERTION_HEADER = "X-Goog-IAP-JWT-Assertion";
    private static final Logger logger = LoggerFactory.getLogger(IapAuthenticationFilter.class);
    private final IapJwtVerifier verifier;
    private final ObjectMapper objectMapper;

    IapAuthenticationFilter(IapJwtVerifier verifier) { this(verifier, new ObjectMapper().registerModule(new JavaTimeModule())); }
    IapAuthenticationFilter(IapJwtVerifier verifier, ObjectMapper objectMapper) { this.verifier = verifier; this.objectMapper = objectMapper; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String assertion = request.getHeader(IAP_ASSERTION_HEADER);
        if (assertion == null || assertion.isBlank()) {
            logger.warn("Rejected IAP request: missing signed assertion requestId={} ({} {})", request.getAttribute(RequestCorrelationFilter.ATTRIBUTE), request.getMethod(), request.getRequestURI());
            reject(request, response, "UNAUTHORIZED", "Es necesario iniciar sesión.", null);
            return;
        }
        try {
            IapJwtClaims claims = verifier.verify(assertion);
            var principal = new AuthenticatedPrincipal(claims.subject(), claims.email(), claims.name());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "N/A",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            SecurityContextHolder.setContext(context);
            try { filterChain.doFilter(request, response); }
            finally { SecurityContextHolder.clearContext(); }
        } catch (IapJwtVerificationException exception) {
            logger.warn("Rejected IAP request: invalid signed assertion requestId={} method={} path={} exceptionType={}", request.getAttribute(RequestCorrelationFilter.ATTRIBUTE), request.getMethod(), request.getRequestURI(), exception.getClass().getName(), SafeLogException.sanitize(exception));
            reject(request, response, "UNAUTHORIZED", "Es necesario iniciar sesión.", exception);
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String code, String message, Throwable exception) throws IOException {
        String id = String.valueOf(request.getAttribute(RequestCorrelationFilter.ATTRIBUTE)); if (id.equals("null")) id = java.util.UUID.randomUUID().toString();
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "IAP");
        response.setHeader(RequestCorrelationFilter.HEADER, id); response.setStatus(HttpStatus.UNAUTHORIZED.value()); response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(Instant.now(), HttpStatus.UNAUTHORIZED.value(), code, message, id));
    }
}
