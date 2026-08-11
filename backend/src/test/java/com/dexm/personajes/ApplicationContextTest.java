package com.dexm.personajes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {
    @Autowired
    private Environment environment;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Test
    void contextLoads() {
    }

    @Test
    void googleOAuthCallbackUsesTheFrontendOrigin() {
        assertThat(environment.getProperty("spring.security.oauth2.client.registration.google.redirect-uri"))
                .isEqualTo("http://localhost:5177/login/oauth2/code/{registrationId}");
    }

    @Test
    void usesAnApplicationSpecificSessionCookie() {
        assertThat(environment.getProperty("server.servlet.session.cookie.name"))
                .isEqualTo("DEXM_SESSION");
    }

    @Test
    void oauth2LoginPersistsTheSecurityContextInTheHttpSession() {
        var oauth2LoginFilter = securityFilterChain.getFilters().stream()
                .filter(OAuth2LoginAuthenticationFilter.class::isInstance)
                .map(OAuth2LoginAuthenticationFilter.class::cast)
                .findFirst()
                .orElseThrow();

        var repository = ReflectionTestUtils.getField(oauth2LoginFilter, "securityContextRepository");
        assertThat(repository).isInstanceOf(DelegatingSecurityContextRepository.class);

        @SuppressWarnings("unchecked")
        var delegates = (List<SecurityContextRepository>) ReflectionTestUtils.getField(repository, "delegates");
        assertThat(delegates).anyMatch(HttpSessionSecurityContextRepository.class::isInstance);
    }

    @Test
    void oauth2LoginAndRefreshManagerShareTheSessionAuthorizedClientRepository() {
        var oauth2LoginFilter = securityFilterChain.getFilters().stream()
                .filter(OAuth2LoginAuthenticationFilter.class::isInstance)
                .map(OAuth2LoginAuthenticationFilter.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(authorizedClientRepository)
                .isInstanceOf(HttpSessionOAuth2AuthorizedClientRepository.class);
        assertThat(ReflectionTestUtils.getField(oauth2LoginFilter, "authorizedClientRepository"))
                .isSameAs(authorizedClientRepository);
        assertThat(authorizedClientManager)
                .isInstanceOf(DefaultOAuth2AuthorizedClientManager.class);
        assertThat(ReflectionTestUtils.getField(authorizedClientManager, "authorizedClientRepository"))
                .isSameAs(authorizedClientRepository);
    }
}
