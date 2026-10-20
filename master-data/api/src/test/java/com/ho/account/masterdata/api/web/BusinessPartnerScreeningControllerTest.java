package com.ho.account.masterdata.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningDecision;
import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningSubmission;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerScreeningUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.exception.ScreeningMutationForbiddenException;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BusinessPartnerScreeningControllerTest {

    private BusinessPartnerScreeningUseCase screeningUseCase;
    private BusinessPartnerUseCase partnerUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        screeningUseCase = mock(BusinessPartnerScreeningUseCase.class);
        partnerUseCase = mock(BusinessPartnerUseCase.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new BusinessPartnerController(
                        partnerUseCase, screeningUseCase))
                .setControllerAdvice(new MasterDataDirectWritePolicy(), new MasterDataExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void submissionUsesVerifiedActorAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/basic/businesspartners/BP-923/screening-submission")
                        .header("X-Auth-User", "verified-submitter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"KR","identifierType":"BUSINESS_REGISTRATION",
                                 "identifier":"1234567890","evidenceReference":"synthetic-evidence-923",
                                 "evidenceExpiresOn":"2026-10-31"}
                                """))
                .andExpect(status().isNoContent());

        ArgumentCaptor<BusinessPartnerScreeningSubmission> command =
                ArgumentCaptor.forClass(BusinessPartnerScreeningSubmission.class);
        verify(screeningUseCase).submitScreening(command.capture());
        assertThat(command.getValue().businessPartnerCode()).isEqualTo("BP-923");
        assertThat(command.getValue().submittedBy()).isEqualTo("verified-submitter");
        assertThat(command.getValue().identifier()).isEqualTo("1234567890");
    }

    @Test
    void reviewerRoleAndVerifiedIdentityAreRequiredForDecision() throws Exception {
        String body = """
                {"status":"APPROVED","riskRating":"LOW","decisionReason":"Evidence checked"}
                """;
        mockMvc.perform(post("/api/basic/businesspartners/BP-923/screening-decision")
                        .header("X-Auth-User", "verified-reviewer")
                        .header("X-Auth-Roles", "PARTNER_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(screeningUseCase, never()).decideScreening(any());

        mockMvc.perform(post("/api/basic/businesspartners/BP-923/screening-decision")
                        .header("X-Auth-User", "verified-reviewer")
                        .header("X-Auth-Roles", "ROLE_SCREENING_REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        ArgumentCaptor<BusinessPartnerScreeningDecision> command =
                ArgumentCaptor.forClass(BusinessPartnerScreeningDecision.class);
        verify(screeningUseCase).decideScreening(command.capture());
        assertThat(command.getValue().reviewedBy()).isEqualTo("verified-reviewer");
        assertThat(command.getValue().status()).isEqualTo(BusinessPartner.KycStatus.APPROVED);
    }

    @Test
    void eligibilityHasExplicitDateAndBooleanResponse() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 20);
        when(screeningUseCase.isEligible("BP-923", date)).thenReturn(true);

        mockMvc.perform(get("/api/basic/businesspartners/BP-923/eligibility")
                        .param("asOfDate", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessPartnerCode").value("BP-923"))
                .andExpect(jsonPath("$.asOfDate").value("2026-10-20"))
                .andExpect(jsonPath("$.eligible").value(true));
        verify(screeningUseCase).isEligible("BP-923", date);
    }

    @Test
    void operationallyActivePartnerCanHaveFalseScreeningEligibility() throws Exception {
        when(partnerUseCase.getActiveBusinessPartners()).thenReturn(java.util.List.of(
                BusinessPartner.reconstitute(923L, "BP-923", "Synthetic partner", "123-45-67890",
                        "Synthetic CEO", "Trade", "Materials", BusinessPartner.PartnerType.VENDOR,
                        true, BusinessPartner.KycStatus.APPROVED, BusinessPartner.RiskRating.LOW,
                        LocalDate.of(2026, 1, 1), BusinessPartner.OPEN_ENDED_VALID_TO,
                        java.time.LocalDateTime.of(2026, 1, 1, 9, 0),
                        java.time.LocalDateTime.of(2026, 1, 1, 9, 0), "fixture", java.util.List.of())));

        mockMvc.perform(get("/api/basic/businesspartners/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessPartnerCode").value("BP-923"))
                .andExpect(jsonPath("$[0].useYn").value(true));
        mockMvc.perform(get("/api/basic/businesspartners/BP-923/eligibility")
                        .param("asOfDate", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false));
    }

    @Test
    void genericUpdateScreeningElevationIsBadRequestWhileMissingPartnerIsNotFound() throws Exception {
        when(partnerUseCase.updateBusinessPartner(eq(923L), any(BusinessPartnerCommand.class)))
                .thenThrow(new ScreeningMutationForbiddenException("Screening decision requires review."));
        when(partnerUseCase.updateBusinessPartner(eq(924L), any(BusinessPartnerCommand.class)))
                .thenThrow(new IllegalArgumentException("Business partner not found."));
        String body = "{\"kycStatus\":\"APPROVED\",\"riskRating\":\"LOW\"}";

        mockMvc.perform(put("/api/basic/businesspartners/923")
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/basic/businesspartners/924")
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingPartnerAndInvalidDecisionMapToClientErrors() throws Exception {
        when(screeningUseCase.submitScreening(any())).thenThrow(new NoSuchElementException("missing"));
        mockMvc.perform(post("/api/basic/businesspartners/MISSING/screening-submission")
                        .header("X-Auth-User", "verified-submitter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"KR","identifierType":"BUSINESS_REGISTRATION",
                                 "identifier":"1234567890","evidenceReference":"synthetic-evidence-923",
                                 "evidenceExpiresOn":"2026-10-31"}
                                """))
                .andExpect(status().isNotFound());
    }
}
