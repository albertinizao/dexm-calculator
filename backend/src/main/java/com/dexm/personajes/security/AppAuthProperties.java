package com.dexm.personajes.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {
    private String mode = "local";
    private String iapAudience = "";

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getIapAudience() { return iapAudience; }
    public void setIapAudience(String iapAudience) { this.iapAudience = iapAudience; }

    public AppAuthMode mode() { return AppAuthMode.from(mode); }

    public void validate() {
        if (mode() == AppAuthMode.IAP && (iapAudience == null || iapAudience.isBlank())) {
            throw new IllegalStateException("APP_IAP_AUDIENCE must be configured when APP_AUTH_MODE=iap");
        }
    }
}
