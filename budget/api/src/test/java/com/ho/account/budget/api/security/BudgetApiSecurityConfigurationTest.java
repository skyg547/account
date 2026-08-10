package com.ho.account.budget.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class BudgetApiSecurityConfigurationTest {

    private static final String SECRET =
            "modern-account-system-super-secret-key-1234567890";

    @Test
    void decoderAcceptsAuthCompatibleHs256TokenAndRejectsWrongIssuer() throws Exception {
        JwtDecoder decoder =
                new BudgetApiSecurityConfiguration().budgetJwtDecoder(SECRET, "auth-service");

        var valid = decoder.decode(token("auth-service"));
        assertThat(valid.getSubject()).isEqualTo("budget-manager");
        assertThat(valid.getClaimAsStringList("roles"))
                .containsExactly("ROLE_BUDGET_MANAGER");

        assertThatThrownBy(() -> decoder.decode(token("forged-issuer")))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void decoderFailsClosedWhenJwtSecretIsMissingOrTooShort() {
        BudgetApiSecurityConfiguration configuration = new BudgetApiSecurityConfiguration();

        assertThatThrownBy(() -> configuration.budgetJwtDecoder("", "auth-service"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
        assertThatThrownBy(() -> configuration.budgetJwtDecoder("short-test-key", "auth-service"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    private String token(String issuer) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("budget-manager")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("roles", List.of("ROLE_BUDGET_MANAGER"))
                .build();
        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return signedJwt.serialize();
    }
}
