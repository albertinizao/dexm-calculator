package com.dexm.personajes.security;

import java.time.Duration;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

public final class NimbusIapJwtVerifier implements IapJwtVerifier {
    static final String IAP_ISSUER = "https://cloud.google.com/iap";
    static final String IAP_JWK_SET_URL = "https://www.gstatic.com/iap/verify/public_key-jwk";
    private final JwtDecoder decoder;

    public NimbusIapJwtVerifier(String audience) {
        this(audience, NimbusJwtDecoder.withJwkSetUri(IAP_JWK_SET_URL)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .cache(new FiveMinuteJwkCache())
                .build());
    }

    NimbusIapJwtVerifier(String audience, NimbusJwtDecoder decoder) {
        var timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(60));
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid IAP audience", null));
        OAuth2TokenValidator<Jwt> requiredClaimsValidator = jwt -> hasText(jwt.getSubject()) && hasText(jwt.getClaimAsString("email"))
                && jwt.getClaims().containsKey("exp") && jwt.getClaims().containsKey("iat")
                && jwt.getExpiresAt() != null && jwt.getIssuedAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "IAP JWT requires sub, email, exp and iat", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(IAP_ISSUER), timestampValidator, audienceValidator, requiredClaimsValidator));
        this.decoder = decoder;
    }

    @Override public IapJwtClaims verify(String assertion) {
        try {
            var rawClaims = SignedJWT.parse(assertion).getJWTClaimsSet();
            if (rawClaims.getIssueTime() == null || rawClaims.getExpirationTime() == null) {
                throw new IapJwtVerificationException("IAP JWT requires exp and iat");
            }
            Jwt jwt = decoder.decode(assertion);
            return new IapJwtClaims(jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"));
        } catch (IapJwtVerificationException exception) {
            throw exception;
        } catch (JwtException exception) {
            throw new IapJwtVerificationException("Invalid IAP JWT assertion", exception);
        } catch (Exception exception) {
            throw new IapJwtVerificationException("Invalid IAP JWT assertion", exception);
        }
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
