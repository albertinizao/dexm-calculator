package com.dexm.personajes.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.http.HttpStatus;
import com.dexm.personajes.adapter.in.web.RequestCorrelationFilter;
import com.dexm.personajes.adapter.in.web.ErrorResponse;
import com.dexm.personajes.adapter.in.web.SafeLogException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import java.time.Instant;

@Configuration
@EnableConfigurationProperties(AppAuthProperties.class)
public class SecurityConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);
    private final AppAuthProperties auth;

    public SecurityConfiguration(AppAuthProperties auth) { this.auth = auth; }

    @PostConstruct
    void validateConfiguration() { auth.validate(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, IapJwtVerifier iapJwtVerifier, ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> writeSecurityError(request, response, objectMapper, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Es necesario iniciar sesión.", exception);
        AccessDeniedHandler deniedHandler = (request, response, exception) -> writeSecurityError(request, response, objectMapper, HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permisos para realizar esta operación.", exception);
        if (auth.mode() == AppAuthMode.LOCAL) {
            return http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                    .addFilterBefore(new LocalAuthenticationFilter(), AnonymousAuthenticationFilter.class)
                    .addFilterBefore(new RequestCorrelationFilter(), LocalAuthenticationFilter.class)
                    .build();
        }
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .addFilterBefore(new IapAuthenticationFilter(iapJwtVerifier), AnonymousAuthenticationFilter.class)
                .addFilterBefore(new RequestCorrelationFilter(), IapAuthenticationFilter.class)
                .build();
    }

    private static void writeSecurityError(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, ObjectMapper mapper, HttpStatus status, String code, String message, Exception exception) throws java.io.IOException {
        String id=String.valueOf(request.getAttribute(RequestCorrelationFilter.ATTRIBUTE)); if(id.equals("null")) id=java.util.UUID.randomUUID().toString();
        log.warn("security_error requestId={} method={} path={} status={} code={} exceptionType={}",id,request.getMethod(),request.getRequestURI(),status.value(),code,exception.getClass().getName(),SafeLogException.sanitize(exception));
        response.setStatus(status.value()); response.setContentType("application/json"); response.setHeader(RequestCorrelationFilter.HEADER,id);
        mapper.writeValue(response.getOutputStream(),new ErrorResponse(Instant.now(),status.value(),code,message,id));
    }

    @Bean
    IapJwtVerifier iapJwtVerifier() {
        return auth.mode() == AppAuthMode.IAP
                ? new NimbusIapJwtVerifier(auth.getIapAudience())
                : assertion -> { throw new IapJwtVerificationException("IAP is disabled in local mode"); };
    }
}
