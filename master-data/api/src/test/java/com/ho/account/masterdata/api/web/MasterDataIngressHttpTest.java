package com.ho.account.masterdata.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerScreeningUseCase;
import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningDecision;
import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningSubmission;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.slf4j.LoggerFactory;

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
    @MockBean private BusinessPartnerScreeningUseCase screeningUseCase;
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
    void screeningCommandsUseVerifiedIdentityAndDedicatedReviewerRole() throws Exception {
        String submissionPath = "/api/basic/businesspartners/BP-923/screening-submission";
        String decisionPath = "/api/basic/businesspartners/BP-923/screening-decision";
        String submission = """
                {"jurisdiction":"KR","identifierType":"BUSINESS_REGISTRATION",
                 "identifier":"1234567890","evidenceReference":"synthetic-evidence"}
                """;
        String decision = """
                {"status":"APPROVED","riskRating":"LOW","decisionReason":"Synthetic evidence reviewed"}
                """;
        String submitter = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60,
                "alice", "ROLE_PARTNER_MANAGER");
        String nonAdmin = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60,
                "alice", "ROLE_USER");
        String reviewer = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60,
                "bob", "ROLE_SCREENING_REVIEWER");

        mockMvc.perform(post(submissionPath).header("X-Auth-User", "alice")
                        .contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(submissionPath).header("Authorization", "Bearer " + submitter)
                        .header("X-Auth-User", "mallory")
                        .contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(submissionPath).header("Authorization", "Bearer " + nonAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isForbidden());
        verifyNoInteractions(screeningUseCase);

        mockMvc.perform(post(submissionPath).header("Authorization", "Bearer " + submitter)
                        .contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isNoContent());
        ArgumentCaptor<BusinessPartnerScreeningSubmission> submitted =
                ArgumentCaptor.forClass(BusinessPartnerScreeningSubmission.class);
        verify(screeningUseCase).submitScreening(submitted.capture());
        assertThat(submitted.getValue().submittedBy()).isEqualTo("alice");

        mockMvc.perform(post(decisionPath).header("Authorization", "Bearer " + submitter)
                        .contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(decisionPath).header("Authorization", "Bearer " + reviewer)
                        .contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isNoContent());
        ArgumentCaptor<BusinessPartnerScreeningDecision> reviewed =
                ArgumentCaptor.forClass(BusinessPartnerScreeningDecision.class);
        verify(screeningUseCase).decideScreening(reviewed.capture());
        assertThat(reviewed.getValue().reviewedBy()).isEqualTo("bob");
    }

    @Test
    void rawDetailRequiresASeparateAuditedGrantEvenForSignedApprovers() throws Exception {
        String path = CHANGE_PATH + "/42/raw-detail";
        Logger controllerLogger = (Logger) LoggerFactory.getLogger(MasterDataChangeRequestController.class);
        Logger ingressLogger = (Logger) LoggerFactory.getLogger(MasterDataIngressAuthenticationFilter.class);
        ListAppender<ILoggingEvent> auditEvents = new ListAppender<>();
        auditEvents.start();
        controllerLogger.addAppender(auditEvents);
        ingressLogger.addAppender(auditEvents);
        try {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get(CHANGE_PATH + "/-1/raw-detail"))
                    .andExpect(status().isUnauthorized());
            for (String role : List.of("ROLE_MASTER_MANAGER", "ROLE_SYSTEM_ADMIN")) {
                String token = userToken(USER_SECRET, "auth-service", USER_AUDIENCE,
                        60, "alice", role);
                mockMvc.perform(get(path)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isForbidden());
            }
            verifyNoInteractions(changeRequestUseCase);
            assertThat(auditEvents.list).hasSize(4);
            assertThat(auditEvents.list.get(0).getFormattedMessage())
                    .isEqualTo("master_data_raw_detail_denied actor=unverified requestId=42");
            assertThat(auditEvents.list.get(1).getFormattedMessage())
                    .isEqualTo("master_data_raw_detail_denied actor=unverified requestId=-1");
            for (ILoggingEvent event : auditEvents.list.subList(2, 4)) {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .isEqualTo("master_data_raw_detail_denied actor=alice requestId=42");
            }
            assertThat(auditEvents.list).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).doesNotContain("payloadJson", "registrationNumber");
            });
        } finally {
            controllerLogger.detachAppender(auditEvents);
            ingressLogger.detachAppender(auditEvents);
            auditEvents.stop();
        }
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
                .andExpect(status().isGone());
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
                .andExpect(status().isGone());
        verifyNoInteractions(fiscalPeriodControlPort);
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
                .andExpect(status().isGone());
        verifyNoInteractions(fiscalPeriodControlPort);
    }

    @Test
    void rejectedCredentialsAndRoleNeverEnterChangeRequestUseCase() throws Exception {
        String manager = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60,
                "alice", "ROLE_MASTER_MANAGER");
        String viewer = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60,
                "alice", "ROLE_VIEWER");

        mockMvc.perform(post(CHANGE_PATH + "/42/approve"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(CHANGE_PATH + "/42/approve")
                        .header("Authorization", "Bearer " + manager)
                        .header("X-Auth-User", "forged-actor"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(CHANGE_PATH + "/42/approve")
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
        verifyNoInteractions(changeRequestUseCase);
    }

    @Test
    void malformedRequestsReturnBadRequestBeforeMutation() throws Exception {
        String manager = userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60,
                "alice", "ROLE_MASTER_MANAGER");

        mockMvc.perform(get("/api/basic/businesspartners/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad request"));
        mockMvc.perform(post(CHANGE_PATH + "/not-a-number/approve")
                        .header("Authorization", "Bearer " + manager))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad request"));
        mockMvc.perform(post(CHANGE_PATH)
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"UNKNOWN_TYPE\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(CHANGE_PATH)
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(CHANGE_PATH)
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(businessPartnerUseCase, changeRequestUseCase);
    }

    @Test
    void missingChangeRequestReturnsNotFound() throws Exception {
        when(changeRequestUseCase.approve(42L, "alice"))
                .thenThrow(new IllegalArgumentException("Change request not found. ID: 42"));

        mockMvc.perform(post(CHANGE_PATH + "/42/approve")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Change request not found"));
        verify(changeRequestUseCase).approve(42L, "alice");
    }

    @Test
    void versionAndInvalidStateConflictsReturnConflict() throws Exception {
        when(changeRequestUseCase.approve(42L, "alice"))
                .thenThrow(new MasterDataVersionConflictException("Version conflict"));
        when(changeRequestUseCase.applyApprovedChange(42L))
                .thenThrow(new IllegalStateException("Change request is not ready to apply. ID: 42"));

        mockMvc.perform(post(CHANGE_PATH + "/42/approve")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Master-data version conflict"));
        mockMvc.perform(post(CHANGE_PATH + "/42/apply")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Change request state conflict"));
        verify(changeRequestUseCase).approve(42L, "alice");
        verify(changeRequestUseCase).applyApprovedChange(42L);
    }

    @Test
    void unexpectedUseCaseFailureReturnsSanitizedServerError() throws Exception {
        String internalDetail = "synthetic-private-failure-detail";
        when(changeRequestUseCase.approve(42L, "alice"))
                .thenThrow(new RuntimeException(internalDetail));

        mockMvc.perform(post(CHANGE_PATH + "/42/approve")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.message", not(containsString(internalDetail))));
        verify(changeRequestUseCase).approve(42L, "alice");
    }

    private static String managerToken() {
        return userToken(USER_SECRET, "auth-service", USER_AUDIENCE, 60,
                "alice", "ROLE_MASTER_MANAGER");
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
