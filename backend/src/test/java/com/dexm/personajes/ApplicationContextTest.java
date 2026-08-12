package com.dexm.personajes;

import com.dexm.personajes.security.AppAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {
    @Autowired private SecurityFilterChain securityFilterChain;
    @Autowired private AppAuthProperties auth;

    @Test void contextLoadsInOpenLocalMode() {
        assertThat(auth.mode().name()).isEqualTo("LOCAL");
        assertThat(securityFilterChain.getFilters())
                .noneMatch(filter -> filter.getClass().getName().contains("OAuth2Login"));
    }
}
