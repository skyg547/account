package com.ho.account.auth.core.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuthModulePropertiesTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    @DisplayName("동적으로 생성한 승인 credential을 가진 configured user를 허용한다")
    void acceptsValidConfiguredUser() {
        AuthModuleProperties properties = validProperties();
        properties.setUsers(List.of(user(randomUsername(), PasswordEncoderPolicy.encode(randomValue(), 4))));

        properties.validateFailClosedPolicy();
    }

    @ParameterizedTest
    @ValueSource(longs = {-17L, 0L, 1L})
    void rejectsJwtTtlBelowTwoSecondsWithoutEchoingConfiguredValue(long expirationSeconds) {
        AuthModuleProperties properties = validProperties();
        properties.getJwt().setExpirationSeconds(expirationSeconds);

        // A one-second TTL can lose its entire usable lifetime when a fractional issue time is serialized.
        assertThatThrownBy(properties::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.jwt.expiration-seconds")
                .hasMessageNotContaining(Long.toString(expirationSeconds));
    }

    @Test
    void acceptsMinimumTwoSecondJwtTtl() {
        AuthModuleProperties properties = validProperties();
        properties.getJwt().setExpirationSeconds(2L);

        assertThatCode(properties::validateFailClosedPolicy).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("configured user 목록의 null 항목을 index 경로와 함께 거부한다")
    void rejectsNullListEntry() {
        AuthModuleProperties properties = validProperties();
        properties.setUsers(java.util.Arrays.asList(user(randomUsername(), PasswordEncoderPolicy.encode(randomValue(), 4)), null));

        assertThatThrownBy(properties::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.users[1]");
    }

    @Test
    @DisplayName("blank username을 index 기반 필드 경로로 거부한다")
    void rejectsBlankUsername() {
        AuthModuleProperties properties = validProperties();
        properties.setUsers(List.of(user("   ", PasswordEncoderPolicy.encode(randomValue(), 4))));

        assertThatThrownBy(properties::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.users[0].username");
    }

    @Test
    @DisplayName("blank password를 index 기반 필드 경로로 거부하고 username을 노출하지 않는다")
    void rejectsBlankPasswordWithoutUsernameDisclosure() {
        AuthModuleProperties properties = validProperties();
        String username = randomUsername();
        properties.setUsers(List.of(user(username, "   ")));

        assertThatThrownBy(properties::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.users[0].password")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain(username));
    }

    @Test
    @DisplayName("raw/noop/unknown/malformed credential을 동일한 비노출 오류 계약으로 거부한다")
    void rejectsInvalidCredentialsWithoutDisclosingInputOrUsername() {
        String username = randomUsername();
        String raw = randomValue();
        String valid = PasswordEncoderPolicy.encode(raw, 4);
        String payload = valid.substring(PasswordEncoderPolicy.BCRYPT_PREFIX.length());
        String unknownId = "x" + randomValue().substring(0, 8);
        List<String> invalid = List.of(
                raw,
                "{noop}" + raw,
                "{" + unknownId + "}" + payload,
                PasswordEncoderPolicy.BCRYPT_PREFIX + randomValue(),
                payload,
                "{BCRYPT}" + payload,
                " " + valid);

        invalid.forEach(candidate -> {
            AuthModuleProperties properties = validProperties();
            properties.setUsers(List.of(user(username, candidate)));

            assertThatThrownBy(properties::validateFailClosedPolicy)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("auth.users[0].password")
                    .hasMessageContaining("{bcrypt}")
                    .satisfies(error -> assertThat(error.getMessage())
                            .doesNotContain(username, raw, unknownId, payload, candidate));
        });
    }

    @Test
    void acceptsValidOtpAndSsoRuntimeConfiguration() {
        AuthModuleProperties properties = baseline();
        properties.getOtp().setEnabled(true);
        properties.getOtp().setUserSecrets(Map.of("alice", "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"));
        properties.getSso().setEnabled(true);
        properties.getSso().setIdentities(List.of(identity("alice", UUID.randomUUID().toString())));

        assertThatCode(properties::validateFailClosedPolicy).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsafeOtpAlgorithmWindowAndMalformedSecret() {
        AuthModuleProperties wrongAlgorithm = baseline();
        wrongAlgorithm.getOtp().setAlgorithm("HmacSHA256");
        assertThatThrownBy(wrongAlgorithm::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.otp.algorithm");

        AuthModuleProperties excessiveWindow = baseline();
        excessiveWindow.getOtp().setToleranceSteps(2);
        assertThatThrownBy(excessiveWindow::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.otp.tolerance-steps");

        AuthModuleProperties malformedSecret = baseline();
        malformedSecret.getOtp().setUserSecrets(Map.of("alice", "not-base32!"));
        assertThatThrownBy(malformedSecret::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base32")
                .hasMessageNotContaining("not-base32!");
    }

    @Test
    void rejectsIncompleteAndDuplicateSsoIdentitiesWithoutLeakingCredential() {
        String credential = UUID.randomUUID().toString();
        AuthModuleProperties incomplete = baseline();
        incomplete.getSso().setIdentities(List.of(identity(" ", credential)));
        assertThatThrownBy(incomplete::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("require provider, credential, and subject")
                .hasMessageNotContaining(credential);

        AuthModuleProperties duplicate = baseline();
        duplicate.getSso().setIdentities(List.of(
                identity("alice", credential),
                identity("bob", credential)));
        assertThatThrownBy(duplicate::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate SSO credentials")
                .hasMessageNotContaining(credential);
    }

    private static AuthModuleProperties validProperties() {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getJwt().setSecret(randomValue());
        properties.getInternalApi().setToken(randomValue());
        return properties;
    }

    private static AuthModuleProperties.User user(String username, String password) {
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }

    private static String randomUsername() {
        return "configured-" + randomValue().substring(0, 12);
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AuthModuleProperties baseline() {
        AuthModuleProperties properties = new AuthModuleProperties();
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(secret));
        properties.getInternalApi().setToken(UUID.randomUUID().toString());
        return properties;
    }

    private AuthModuleProperties.SsoIdentity identity(String subject, String credential) {
        AuthModuleProperties.SsoIdentity identity = new AuthModuleProperties.SsoIdentity();
        identity.setProvider("corporate-oidc");
        identity.setSubject(subject);
        identity.setCredential(credential);
        return identity;
    }
}
