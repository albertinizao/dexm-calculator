package com.dexm.personajes.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NimbusIapJwtVerifierTest {
    private static final String AUDIENCE = "/projects/123/locations/europe-southwest1/services/dexm-calculator";
    private ECKey signingKey;
    private NimbusIapJwtVerifier verifier;

    @BeforeEach void setUp() throws Exception {
        signingKey = new ECKeyGenerator(Curve.P_256).keyID("fixture-key").generate();
        var source = new ImmutableJWKSet<SecurityContext>(new com.nimbusds.jose.jwk.JWKSet(signingKey.toPublicJWK()));
        var decoder = NimbusJwtDecoder.withJwkSource(source).jwsAlgorithm(SignatureAlgorithm.ES256).build();
        verifier = new NimbusIapJwtVerifier(AUDIENCE, decoder);
    }

    @Test void acceptsSignedEs256IapFixtureWithExpectedClaims() throws Exception {
        var claims = verifier.verify(token(AUDIENCE, NimbusIapJwtVerifier.IAP_ISSUER, Instant.now().minusSeconds(5), Instant.now().plusSeconds(300)));
        assertThat(claims).isEqualTo(new IapJwtClaims("stable-subject", "user@example.com", "User"));
    }

    @Test void rejectsSignedFixtureWithWrongAudience() throws Exception {
        assertThatThrownBy(() -> verifier.verify(token("wrong-audience", NimbusIapJwtVerifier.IAP_ISSUER, Instant.now().minusSeconds(5), Instant.now().plusSeconds(300))))
                .isInstanceOf(IapJwtVerificationException.class);
    }

    @Test void rejectsUnsignedOrWronglySignedFixture() throws Exception {
        var otherKey = new ECKeyGenerator(Curve.P_256).keyID("other-key").generate();
        assertThatThrownBy(() -> verifier.verify(token(otherKey, AUDIENCE, NimbusIapJwtVerifier.IAP_ISSUER, Instant.now().minusSeconds(5), Instant.now().plusSeconds(300))))
                .isInstanceOf(IapJwtVerificationException.class);
    }

    @Test void rejectsFixtureWithoutIssuedAt() throws Exception {
        var payload = new JWTClaimsSet.Builder().subject("stable-subject").claim("email", "user@example.com")
                .issuer(NimbusIapJwtVerifier.IAP_ISSUER).audience(AUDIENCE).expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).build(), payload);
        jwt.sign(new ECDSASigner(signingKey));
        assertThatThrownBy(() -> verifier.verify(jwt.serialize())).isInstanceOf(IapJwtVerificationException.class);
    }

    private String token(String audience, String issuer, Instant issuedAt, Instant expiresAt) throws Exception {
        return token(signingKey, audience, issuer, issuedAt, expiresAt);
    }

    private static String token(ECKey key, String audience, String issuer, Instant issuedAt, Instant expiresAt) throws Exception {
        var payload = new JWTClaimsSet.Builder().subject("stable-subject").claim("email", "user@example.com").claim("name", "User")
                .issuer(issuer).audience(audience).issueTime(Date.from(issuedAt)).expirationTime(Date.from(expiresAt)).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).build(), payload);
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }
}
