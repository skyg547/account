package com.ho.account.masterdata.api.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Exercises the real servlet filter, MVC advice, controllers, and use-case boundary together. */
@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.cloud.vault.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
class MasterDataIngressHttpTest {

    private static final String USER_SECRET = randomSecret();
    private static final String SERVICE_SECRET = randomSecret();
    private static final String USER_AUDIENCE = "master-data-test-api";
    private static final String CHANGE_PATH = "/api/master-data/change-requests";
    private static final String STATUS_PATH = "/api/internal/fiscal-periods/1/closing-status";

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> USER_SECRET);
        registry.add("auth.jwt.audience", () -> USER_AUDIENCE);
        registry.add("master-data.internal-auth.secret", () -> SERVICE_SECRET);
    }

    @Autowired private MockMvc mockMvc;
    @MockBean private BusinessPartnerUseCase businessPartnerUseCase;
    @MockBean private MasterDataChangeRequestUseCase changeRequestUseCase;
    @MockBean private FiscalPeriodControlPort fiscalPeriodControlPort;

    @Test
    void rawForwardedIdentityCannotReachMutationOrApprovalUseCases() throws Exception {
        mockMvc.perform(delete("/api/basic/businesspartners/1")
                        .header("X-Auth-User", "admin")
                        .header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(CHANGE_PATH + "/1/approve")
                        .header("X-Auth-User", "admin")
                        .header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(CHANGE_PATH + "/pending")
                        .header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(businessPartnerUseCase, changeRequestUseCase);
    }

    @Test
    void nonRootContextPathStillRejectsHeaderOnlyMutation() throws Exception {
        mockMvc.perform(delete("/master-data/api/basic/businesspartners/1")
                        .contextPath("/master-data")
                        .header("X-Auth-User", "admin")
                        .header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(businessPartnerUseCase);
    }

    @Test
    void matrixAndEncodedPathsCannotBypassCredentialChecks() throws Exception {
        for (String path : List.of(
                "/api;v=1/basic/businesspartners/1",
                "/api%3Bv=1/basic/businesspartners/1",
                "/api%2Fbasic/businesspartners/1",
                "/api/../api/basic/businesspartners/1")) {
            mockMvc.perform(delete(path)
                            .header("X-Auth-User", "admin")
                            .header("X-Auth-Roles", "ROLE_ADMIN"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(delete("/master-data/api;v=1/basic/businesspartners/1")
                        .contextPath("/master-data")
                        .header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/master-data;v=1/change-requests/pending")
                        .header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ap%69/master-data/change-requests/pending")
                        .header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/internal;v=1/fiscal-periods/1/closing-status")
                        .header("X-Service-Identity", "closing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\",\"auditUser\":\"mallory\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(businessPartnerUseCase, changeRequestUseCase, fiscalPeriodControlPort);
    }

    @Test
    void signedUserTokenBindsHeaderCaseAndRolesBeforeUseCase() throws Exception {
        String token = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60, "alice", "ROLE_ADMIN");
        mockMvc.perform(delete("/api/basic/businesspartners/1")
                        .header("Authorization", "Bearer " + token)
                        .header("x-auth-user", "mallory")
                        .header("x-auth-roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/basic/businesspartners/1")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Auth-User", "alice")
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/basic/businesspartners/1")
                        .header("Authorization", "Bearer " + token)
                        .header("x-auth-role-version", "999"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/basic/businesspartners/1")
                        .header("Authorization", "Bearer " + token)
                        .header("x-auth-privileged", "true"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(businessPartnerUseCase);

        mockMvc.perform(delete("/api/basic/businesspartners/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(businessPartnerUseCase).deleteBusinessPartner(1L);
    }

    @Test
    void invalidSignatureIssuerExpiryAndAudienceNeverReachUseCase() throws Exception {
        List<String> tokens = List.of(
                userToken(randomSecret(), "auth-service", USER_AUDIENCE, 60, "alice", "ROLE_ADMIN"),
                userToken(USER_SECRET, "wrong-issuer", USER_AUDIENCE, 60, "alice", "ROLE_ADMIN"),
                userToken(USER_SECRET, "auth-service", "account-api", 60, "alice", "ROLE_ADMIN"),
                userToken(USER_SECRET, "auth-service", USER_AUDIENCE, -1, "alice", "ROLE_ADMIN"));
        for (String token : tokens) {
            mockMvc.perform(delete("/api/basic/businesspartners/1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(businessPartnerUseCase);
    }

    @Test
    void preflightDoesNotInvokeProtectedUseCases() throws Exception {
        mockMvc.perform(options(CHANGE_PATH + "/1/approve"))
                .andExpect(status().isOk());
        verifyNoInteractions(changeRequestUseCase);
    }

    @Test
    void missingVerifierConfigurationFailsClosedForMutations() throws Exception {
        MockMvc noKeys = MockMvcBuilders.standaloneSetup(new BusinessPartnerController(businessPartnerUseCase))
                .addFilters(new MasterDataIngressAuthenticationFilter("", "auth-service", USER_AUDIENCE, ""))
                .build();
        String token = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60, "alice", "ROLE_ADMIN");
        noKeys.perform(delete("/api/basic/businesspartners/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        MockMvc noAudience = MockMvcBuilders.standaloneSetup(new BusinessPartnerController(businessPartnerUseCase))
                .addFilters(new MasterDataIngressAuthenticationFilter(
                        USER_SECRET, "auth-service", "", SERVICE_SECRET))
                .build();
        noAudience.perform(delete("/api/basic/businesspartners/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(businessPartnerUseCase);
    }

    @Test
    void signedRolePolicyAndApprovalActorAreEnforced() throws Exception {
        String viewer = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60, "alice", "ROLE_VIEWER");
        mockMvc.perform(delete("/api/basic/businesspartners/1")
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(CHANGE_PATH + "/1/approve")
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
        verifyNoInteractions(businessPartnerUseCase, changeRequestUseCase);

        MasterDataChangeRequest approved = new MasterDataChangeRequest(
                MasterDataType.BUSINESS_PARTNER, "BP-1", ChangeType.UPDATE,
                LocalDate.of(2026, 10, 1), 2, "requester", "update", "{}");
        approved.approve("alice");
        when(changeRequestUseCase.approve(1L, "alice")).thenReturn(approved);
        String manager = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60, "alice", "ROLE_MASTER_MANAGER");
        mockMvc.perform(post(CHANGE_PATH + "/1/approve")
                        .header("Authorization", "Bearer " + manager))
                .andExpect(status().isOk());
        verify(changeRequestUseCase).approve(1L, "alice");
    }

    @Test
    void closingAssertionControlsServiceAndDelegatedActorNotBodyAuditUser() throws Exception {
        when(fiscalPeriodControlPort.updateClosingStatus(1L, "CLOSED", "closing:alice"))
                .thenReturn(new FiscalPeriodRef(1L, "2026", "10", LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31), "CLOSED"));
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Identity", "closing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\",\"auditUser\":\"mallory\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Assertion", serviceToken("other-api", List.of("ROLE_ACCOUNTING_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Assertion", serviceToken("master-data-internal", List.of("ROLE_VIEWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Identity", "another-service")
                        .header("X-Service-Assertion", serviceToken("master-data-internal", List.of("ROLE_ACCOUNTING_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Assertion", serviceToken("master-data-internal",
                                List.of("ROLE_ACCOUNTING_ADMIN"), randomSecret(), "closing", -1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Assertion", serviceToken("master-data-internal",
                                List.of("ROLE_ACCOUNTING_ADMIN"), SERVICE_SECRET, "other-service", 60))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Assertion", serviceToken("master-data-internal",
                                List.of("ROLE_ACCOUNTING_ADMIN"), SERVICE_SECRET, "closing", -1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Assertion", serviceToken("master-data-internal",
                                List.of("ROLE_ACCOUNTING_ADMIN"), SERVICE_SECRET, "closing", 600))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/internal/fiscal-periods/2/closing-status")
                        .header("X-Service-Assertion", serviceToken("master-data-internal",
                                List.of("ROLE_ACCOUNTING_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Assertion", serviceToken("master-data-internal",
                                List.of("ROLE_ACCOUNTING_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"OPEN\"}"))
                .andExpect(status().isUnauthorized());
        for (String assertion : List.of(
                serviceTokenWithTarget(1L, "CLOSED", "POST", STATUS_PATH),
                serviceTokenWithTarget(1L, "CLOSED", "PUT", "/api/internal/fiscal-periods/2/closing-status"),
                serviceTokenWithTarget("1", "CLOSED", "PUT", STATUS_PATH),
                serviceTokenWithTarget(new BigInteger("9223372036854775808"), "CLOSED", "PUT", STATUS_PATH))) {
            mockMvc.perform(put(STATUS_PATH)
                            .header("X-Service-Assertion", assertion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"closingStatus\":\"CLOSED\"}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(fiscalPeriodControlPort);
        mockMvc.perform(put(STATUS_PATH)
                        .header("X-Service-Identity", "closing")
                        .header("X-Service-Assertion", serviceToken("master-data-internal", List.of("ROLE_ACCOUNTING_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\",\"auditUser\":\"mallory\"}"))
                .andExpect(status().isOk());
        verify(fiscalPeriodControlPort).updateClosingStatus(1L, "CLOSED", "closing:alice");
    }

    @Test
    void validClosingAssertionUsesContextRelativeSignedPath() throws Exception {
        when(fiscalPeriodControlPort.updateClosingStatus(1L, "CLOSED", "closing:alice"))
                .thenReturn(new FiscalPeriodRef(1L, "2026", "10", LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31), "CLOSED"));
        mockMvc.perform(put("/master-data" + STATUS_PATH)
                        .contextPath("/master-data")
                        .header("X-Service-Assertion", serviceToken("master-data-internal",
                                List.of("ROLE_ACCOUNTING_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"CLOSED\"}"))
                .andExpect(status().isOk());
        verify(fiscalPeriodControlPort).updateClosingStatus(1L, "CLOSED", "closing:alice");
    }

    private static String userToken(String secret, String issuer, String audience, long ttlSeconds,
            String actor, String role) {
        Instant now = Instant.now();
        return Jwts.builder().setSubject(actor).claim("roles", List.of(role)).claim("roleVersion", 1L)
                .setIssuer(issuer).setAudience(audience)
                .setIssuedAt(Date.from(now.minusSeconds(120)))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private static String serviceToken(String audience, List<String> actorRoles) {
        return serviceToken(audience, actorRoles, SERVICE_SECRET, "closing", 60);
    }

    private static String serviceToken(String audience, List<String> actorRoles,
            String secret, String service, long ttlSeconds) {
        return serviceToken(audience, actorRoles, secret, service, ttlSeconds,
                1L, "CLOSED", "PUT", STATUS_PATH);
    }

    private static String serviceTokenWithTarget(Object periodId, String status, String method, String path) {
        return serviceToken("master-data-internal", List.of("ROLE_ACCOUNTING_ADMIN"),
                SERVICE_SECRET, "closing", 60, periodId, status, method, path);
    }

    private static String serviceToken(String audience, List<String> actorRoles,
            String secret, String service, long ttlSeconds,
            Object periodId, String status, String method, String path) {
        Instant now = Instant.now();
        return Jwts.builder().setSubject(service).claim("actor", "alice")
                .claim("actorRoles", actorRoles)
                .claim("fiscalPeriodId", periodId)
                .claim("closingStatus", status)
                .claim("method", method)
                .claim("path", path)
                .setIssuer("closing-service").setAudience(audience)
                .setIssuedAt(Date.from(now.minusSeconds(120)))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
