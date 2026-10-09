package com.ho.account.auth.api.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.model.AdminUserView;
import com.ho.account.auth.core.application.port.in.AdminUserQueryUseCase;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminUserControllerTest {

    private static final String SECRET = randomSecret();
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> SECRET);
        registry.add("auth.internal-api.token", AdminUserControllerTest::randomSecret);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserQueryUseCase adminUserQueryUseCase;

    @MockBean
    private AuthUseCase authUseCase;

    @Test
    void signedAdminTokenPassesRealFilterAndController() throws Exception {
        when(adminUserQueryUseCase.findAllUsers("admin")).thenReturn(List.of(new AdminUserView(
                281_474_976_710_656L, "alpha", "alpha", "ACCOUNTING_ADMIN", "ACTIVE", "", "FIN")));
        when(authUseCase.validateTokenVersion("admin", 1L)).thenReturn(true);
        String token = token("auth-service", "account-api", List.of("ROLE_SYSTEM_ADMIN"),
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(600), SIGNING_KEY);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Auth-User", "admin")
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN")
                        .header("X-Auth-Role-Version", "1")
                        .header("X-Auth-Department", "FIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(281_474_976_710_656L))
                .andExpect(jsonPath("$[0].role").value("ACCOUNTING_ADMIN"));
        verify(adminUserQueryUseCase).findAllUsers("admin");
    }

    @Test
    void signedAdminTokenRejectsCallerWithoutStoredGlobalSystemAdminAssignment() throws Exception {
        when(adminUserQueryUseCase.findAllUsers("admin"))
                .thenThrow(new UserAccessDeniedException("System administrator role is required."));
        when(authUseCase.validateTokenVersion("admin", 1L)).thenReturn(true);
        String token = token("auth-service", "account-api", List.of("ROLE_SYSTEM_ADMIN"),
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(600), SIGNING_KEY);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Auth-User", "admin")
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN")
                        .header("X-Auth-Role-Version", "1")
                        .header("X-Auth-Department", "FIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));

        verify(adminUserQueryUseCase).findAllUsers("admin");
    }

    @Test
    void rawAdminHeaderWithoutBearerCannotReachUseCase() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Auth-Error", "ACCESS_TOKEN_INVALID"));
        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void matrixAndEncodedAdminPathsCannotBypassBearerFilter() throws Exception {
        for (String path : List.of("/api/admin;v=1/users", "/api/admin%3Bv=1/users")) {
            mockMvc.perform(get(path).header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-Auth-Error", "ACCESS_TOKEN_INVALID"));
        }
        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void signedNonAdminTokenGetsForbiddenAdviceResponse() throws Exception {
        when(authUseCase.validateTokenVersion("admin", 1L)).thenReturn(true);
        String token = token("auth-service", "account-api", List.of("ROLE_AUDITOR"),
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(600), SIGNING_KEY);
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));
        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void spoofedRoleHeaderWithValidNonAdminBearerIsRejected() throws Exception {
        String token = token("auth-service", "account-api", List.of("ROLE_USER"),
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(600), SIGNING_KEY);
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void mismatchedForwardedIdentityHeadersAreRejected() throws Exception {
        String token = token("auth-service", "account-api", List.of("ROLE_SYSTEM_ADMIN"),
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(600), SIGNING_KEY);
        for (Map.Entry<String, String> mismatch : Map.of(
                "X-Auth-User", "other-user",
                "X-Auth-Role-Version", "2",
                "X-Auth-Department", "OTHER",
                "X-User-ID", "other-user").entrySet()) {
            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + token)
                            .header(mismatch.getKey(), mismatch.getValue()))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void multipleAuthorizationHeadersAreRejected() throws Exception {
        String token = token("auth-service", "account-api", List.of("ROLE_SYSTEM_ADMIN"),
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(600), SIGNING_KEY);
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void revokedRoleVersionCannotReachAdminQuery() throws Exception {
        String token = token("auth-service", "account-api", List.of("ROLE_SYSTEM_ADMIN"),
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(600), SIGNING_KEY);
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verify(authUseCase).validateTokenVersion("admin", 1L);
        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void invalidSignatureIssuerAudienceAndExpirationAreRejected() throws Exception {
        Instant now = Instant.now();
        List<String> invalidTokens = List.of(
                token("auth-service", "account-api", List.of("ROLE_SYSTEM_ADMIN"),
                        now.minusSeconds(5), now.plusSeconds(600), Keys.hmacShaKeyFor(randomSecret().getBytes(StandardCharsets.UTF_8))),
                token("other-issuer", "account-api", List.of("ROLE_SYSTEM_ADMIN"),
                        now.minusSeconds(5), now.plusSeconds(600), SIGNING_KEY),
                token("auth-service", "other-audience", List.of("ROLE_SYSTEM_ADMIN"),
                        now.minusSeconds(5), now.plusSeconds(600), SIGNING_KEY),
                token("auth-service", "account-api", List.of("ROLE_SYSTEM_ADMIN"),
                        now.minusSeconds(600), now.minusSeconds(5), SIGNING_KEY));
        for (String invalid : invalidTokens) {
            mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void loginRemainsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"x\",\"loginType\":\"NORMAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void adminPreflightDoesNotRequireBearer() throws Exception {
        mockMvc.perform(options("/api/admin/users"))
                .andExpect(status().isOk());
        verifyNoInteractions(adminUserQueryUseCase);
    }

    private static String token(
            String issuer, String audience, List<String> roles, Instant issuedAt, Instant expiration, Key key) {
        return Jwts.builder()
                .setSubject("admin")
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiration))
                .claim("roles", roles)
                .claim("roleVersion", 1)
                .claim("departmentCode", "FIN")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
