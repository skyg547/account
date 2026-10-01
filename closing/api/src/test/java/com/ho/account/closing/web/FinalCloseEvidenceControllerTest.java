package com.ho.account.closing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase;
import com.ho.account.closing.application.service.FinalCloseEvidenceValidationException;
import com.ho.account.closing.application.service.FinalCloseEvidenceProperties;
import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Outcome;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Type;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FinalCloseEvidenceControllerTest {
    private FinalCloseEvidenceUseCase useCase;
    private FinalCloseEvidenceProperties properties;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(FinalCloseEvidenceUseCase.class);
        properties = new FinalCloseEvidenceProperties();
        mvc = MockMvcBuilders.standaloneSetup(new FinalCloseEvidenceController(useCase, properties))
                .setControllerAdvice(new ClosingExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().modules(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .build();
    }

    @Test
    void providerCreatesEvidenceAndTrustedHeaderActorOverridesUntrustedBodyField() throws Exception {
        properties.setTrustedSubmitters(Set.of("trusted-provider"));
        FinalCloseEvidenceSet recorded = evidence("trusted-provider");
        when(useCase.record(eq(10L), any(), eq("trusted-provider"))).thenReturn(recorded);

        mvc.perform(post("/api/closing/calendars/10/final-close-evidence")
                .header("X-Auth-User", " trusted-provider ")
                .header("X-Auth-Roles", "closing_evidence_provider")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenceSetId").value("evidence-1"))
                .andExpect(jsonPath("$.submittedBy").value("trusted-provider"))
                .andExpect(jsonPath("$.controlCount").value(1));
        verify(useCase).record(eq(10L), any(), eq("trusted-provider"));
    }

    @Test
    void defaultEmptyTrustedSubmitterConfigurationRejectsProviderRole() throws Exception {
        mvc.perform(post("/api/closing/calendars/10/final-close-evidence")
                .header("X-Auth-User", "provider")
                .header("X-Auth-Roles", "ROLE_CLOSING_EVIDENCE_PROVIDER")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    void unlistedActorIsForbiddenEvenWithProviderRole() throws Exception {
        properties.setTrustedSubmitters(Set.of("listed-provider"));
        mvc.perform(post("/api/closing/calendars/10/final-close-evidence")
                .header("X-Auth-User", "unlisted-provider")
                .header("X-Auth-Roles", "ROLE_CLOSING_EVIDENCE_PROVIDER")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    void missingActorIsUnauthorizedAndOrdinaryOrAdminRolesAreForbidden() throws Exception {
        properties.setTrustedSubmitters(Set.of("actor"));
        mvc.perform(post("/api/closing/calendars/10/final-close-evidence")
                .header("X-Auth-Roles", "ROLE_CLOSING_EVIDENCE_PROVIDER")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isUnauthorized());
        for (String role : List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_ACCOUNTING_ADMIN")) {
            mvc.perform(post("/api/closing/calendars/10/final-close-evidence")
                    .header("X-Auth-User", "actor").header("X-Auth-Roles", role)
                    .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(useCase);
    }

    @Test
    void beanValidationRejectsMalformedProviderPayload() throws Exception {
        properties.setTrustedSubmitters(Set.of("provider"));
        mvc.perform(post("/api/closing/calendars/10/final-close-evidence")
                .header("X-Auth-User", "provider")
                .header("X-Auth-Roles", "ROLE_CLOSING_EVIDENCE_PROVIDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("\"evidence-1\"", "\"\"").replace("\"a\".repeat(64)", "\"bad\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    @Test
    void evidenceValidationFailureUsesDedicatedConflictCode() throws Exception {
        properties.setTrustedSubmitters(Set.of("provider"));
        when(useCase.record(eq(10L), any(), eq("provider")))
                .thenThrow(new FinalCloseEvidenceValidationException("newer evidence failed"));
        mvc.perform(post("/api/closing/calendars/10/final-close-evidence")
                .header("X-Auth-User", "provider")
                .header("X-Auth-Roles", "ROLE_CLOSING_EVIDENCE_PROVIDER")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FINAL_CLOSE_EVIDENCE_BLOCKED"));
    }

    private String validJson() {
        return """
                {"evidenceSetId":"evidence-1","calendarId":10,"fiscalPeriodId":20,
                 "fiscalYear":"2026","fiscalPeriod":"01","ledgerCutoff":"2026-01-31",
                 "observedAt":"2026-02-01T00:00:00Z","submittedBy":"forged-body",
                 "contentDigest":"%s","controls":[{"type":"AP_SUBLEDGER","sourceSystem":"payable",
                 "sourceRunId":"run-1","outcome":"PASS","blockingItemCount":0,
                 "totals":[{"accountCode":"1000","currencyCode":"KRW","sourceTotal":0,"postedTotal":0}]}]}
                """.formatted("a".repeat(64));
    }

    private FinalCloseEvidenceSet evidence(String actor) {
        return new FinalCloseEvidenceSet("evidence-1", 10L, 20L, "2026", "01", LocalDate.of(2026, 1, 31),
                Instant.parse("2026-02-01T00:00:00Z"), actor, "a".repeat(64),
                List.of(new FinalCloseEvidenceControl(Type.AP_SUBLEDGER, "payable", "run-1", Outcome.PASS, 0,
                        List.of(new FinalCloseEvidenceTotal("1000", "KRW", BigDecimal.ZERO, BigDecimal.ZERO)))));
    }
}
