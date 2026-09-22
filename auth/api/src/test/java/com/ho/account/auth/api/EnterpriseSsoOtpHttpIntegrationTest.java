package com.ho.account.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = AuthApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:enterprise-sso-otp;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "auth.persistence.mode=jpa",
                "auth.master-data.enabled=false",
                "auth.login-security.store=memory",
                "auth.login-security.max-failures=2",
                "auth.login-security.lock-duration-minutes=15",
                "auth.sso.enabled=true",
                "auth.sso.default-provider=corporate-oidc",
                "auth.otp.enabled=true",
                "auth.otp.algorithm=HmacSHA1",
                "auth.otp.digits=6",
                "auth.otp.period-seconds=30",
                "auth.otp.tolerance-steps=1"
        })
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EnterpriseSsoOtpHttpIntegrationTest {

    private static final String USERNAME = "ssointegration";
    private static final String PROVIDER = "corporate-oidc";
    private static final String INTERNAL_ROLE = "ROLE_INTERNAL_ACCOUNTING";
    private static final String PROVIDER_ROLE = "ROLE_UNTRUSTED_PROVIDER_ADMIN";
    // RFC 6238 Appendix B public SHA-1 secret, Base32 encoded.
    private static final String RFC_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private static final byte[] RFC_SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    private static final String SSO_CREDENTIAL = UUID.randomUUID().toString();
    private static final String JWT_SECRET = ephemeralValue();
    private static final String INTERNAL_TOKEN = ephemeralValue();
    private static final String STORED_PASSWORD = PasswordEncoderPolicy.encode(UUID.randomUUID().toString(), 4);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void enterpriseAuthenticationProperties(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> JWT_SECRET);
        registry.add("auth.internal-api.token", () -> INTERNAL_TOKEN);
        registry.add("auth.users[0].username", () -> USERNAME);
        registry.add("auth.users[0].password", () -> STORED_PASSWORD);
        registry.add("auth.users[0].department-code", () -> "FIN");
        registry.add("auth.users[0].roles[0]", () -> INTERNAL_ROLE);
        registry.add("auth.sso.identities[0].provider", () -> PROVIDER);
        registry.add("auth.sso.identities[0].credential", () -> SSO_CREDENTIAL);
        registry.add("auth.sso.identities[0].subject", () -> USERNAME);
        registry.add("auth.sso.identities[0].email", () -> "ssointegration@example.test");
        registry.add("auth.sso.identities[0].name", () -> "SSO Integration User");
        registry.add("auth.sso.identities[0].department-code", () -> "FIN");
        registry.add("auth.sso.identities[0].roles[0]", () -> PROVIDER_ROLE);
        registry.add("auth.otp.user-secrets." + USERNAME, () -> RFC_SECRET_BASE32);
    }

    @Test
    void configuredSsoAndTotpFlowIssuesInternalRoleJwtThenFailsClosedAndLocks() throws Exception {
        String otpCode = currentTotp();
        String successBody = loginBody(otpCode, true);

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(successBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value(INTERNAL_ROLE))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String compactJwt = objectMapper.readTree(token).get("token").asText();
        String[] jwtSegments = compactJwt.split("\\.");
        assertThat(jwtSegments).hasSize(3);
        var claims = objectMapper.readTree(Base64.getUrlDecoder().decode(jwtSegments[1]));
        assertThat(claims.get("sub").asText()).isEqualTo(USERNAME);
        List<String> jwtRoles = objectMapper.readerForListOf(String.class).readValue(claims.get("roles"));
        assertThat(jwtRoles)
                .containsExactly(INTERNAL_ROLE)
                .doesNotContain(PROVIDER_ROLE);

        String invalidOtp = invalidTotpForCurrentWindow();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(invalidOtp, true)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(null, false)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(currentTotp(), true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));
    }

    private String loginBody(String otpCode, boolean includeOtp) throws Exception {
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("username", USERNAME);
        request.put("password", SSO_CREDENTIAL);
        request.put("loginType", "SSO");
        request.put("ssoProvider", PROVIDER);
        if (includeOtp) {
            request.put("otpCode", otpCode);
        }
        return objectMapper.writeValueAsString(request);
    }

    private String currentTotp() throws Exception {
        long counter = Math.floorDiv(Instant.now().getEpochSecond(), 30L);
        return totpForCounter(counter);
    }

    private String invalidTotpForCurrentWindow() throws Exception {
        long currentCounter = Math.floorDiv(Instant.now().getEpochSecond(), 30L);
        Set<String> acceptedWindowCodes = new HashSet<>();
        // Include one extra step on each side so a clock rollover between generation and HTTP handling stays invalid.
        for (long counter = Math.max(0L, currentCounter - 2L); counter <= currentCounter + 2L; counter++) {
            acceptedWindowCodes.add(totpForCounter(counter));
        }
        for (int candidate = 0; candidate <= 999_999; candidate++) {
            String code = String.format(Locale.ROOT, "%06d", candidate);
            if (!acceptedWindowCodes.contains(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to select a six-digit code outside the active TOTP window");
    }

    private String totpForCounter(long counter) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(RFC_SECRET, "HmacSHA1"));
        byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
        int offset = digest[digest.length - 1] & 0x0f;
        int binary = ((digest[offset] & 0x7f) << 24)
                | ((digest[offset + 1] & 0xff) << 16)
                | ((digest[offset + 2] & 0xff) << 8)
                | (digest[offset + 3] & 0xff);
        return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
    }

    private static String ephemeralValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
