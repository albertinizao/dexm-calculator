package com.dexm.personajes.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppAuthPropertiesTest {
    @Test void iapModeFailsClosedWithoutAudience() {
        var properties = new AppAuthProperties();
        properties.setMode("iap");
        properties.setIapAudience("");
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_IAP_AUDIENCE");
    }

    @Test void rejectsUnknownMode() {
        var properties = new AppAuthProperties();
        properties.setMode("oauth2");
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_AUTH_MODE");
    }
}
