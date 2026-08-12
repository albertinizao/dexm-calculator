package com.dexm.personajes.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

@Configuration
@EnableConfigurationProperties(AppAuthProperties.class)
public class SecurityConfiguration {
    private final AppAuthProperties auth;

    public SecurityConfiguration(AppAuthProperties auth) { this.auth = auth; }

    @PostConstruct
    void validateConfiguration() { auth.validate(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, IapJwtVerifier iapJwtVerifier) throws Exception {
        if (auth.mode() == AppAuthMode.LOCAL) {
            return http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                    .addFilterBefore(new LocalAuthenticationFilter(), AnonymousAuthenticationFilter.class)
                    .build();
        }
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(HttpStatus.FORBIDDEN.value())))
                .addFilterBefore(new IapAuthenticationFilter(iapJwtVerifier), AnonymousAuthenticationFilter.class)
                .build();
    }

    @Bean
    IapJwtVerifier iapJwtVerifier() {
        return auth.mode() == AppAuthMode.IAP
                ? new NimbusIapJwtVerifier(auth.getIapAudience())
                : assertion -> { throw new IapJwtVerificationException("IAP is disabled in local mode"); };
    }
}
