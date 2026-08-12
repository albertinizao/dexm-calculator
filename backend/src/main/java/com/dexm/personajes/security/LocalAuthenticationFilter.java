package com.dexm.personajes.security;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Creates a stable, full-access identity for development only. */
final class LocalAuthenticationFilter extends OncePerRequestFilter {
    static final AuthenticatedPrincipal LOCAL_PRINCIPAL = new AuthenticatedPrincipal(
            "local-development-admin", "local@dexm.invalid", "Local development administrator");

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(LOCAL_PRINCIPAL, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(context);
        try { filterChain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
