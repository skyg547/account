package com.ho.account.masterdata.api.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BusinessPartnerControllerTest {

    private BusinessPartnerUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(BusinessPartnerUseCase.class);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new BusinessPartnerController(useCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getByCodeUsesCanonicalRestPathAndReturnsUseCaseDtoFields() throws Exception {
        BusinessPartner partner = BusinessPartner.reconstitute(
                42L,
                "BP-042",
                "한빛상사",
                "123-45-67890",
                "김한빛",
                "도매업",
                "전자부품",
                BusinessPartner.PartnerType.VENDOR,
                true,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.MEDIUM,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(9999, 12, 31),
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 7, 1, 10, 30),
                "master-data-admin",
                List.of());
        when(useCase.getBusinessPartnerByCode("BP-042")).thenReturn(Optional.of(partner));

        mockMvc.perform(get("/api/basic/businesspartners/{businessPartnerCode}", "BP-042"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.businessPartnerCode").value("BP-042"))
                .andExpect(jsonPath("$.businessPartnerName").value("한빛상사"))
                .andExpect(jsonPath("$.registrationNumber").value("123-45-*****"))
                .andExpect(jsonPath("$.ceoName").value("김한빛"))
                .andExpect(jsonPath("$.businessType").value("도매업"))
                .andExpect(jsonPath("$.businessItem").value("전자부품"))
                .andExpect(jsonPath("$.partnerType").value("VENDOR"))
                .andExpect(jsonPath("$.useYn").value(true))
                .andExpect(jsonPath("$.kycStatus").value("APPROVED"))
                .andExpect(jsonPath("$.riskRating").value("MEDIUM"))
                .andExpect(jsonPath("$.validFrom").value("2026-07-01"))
                .andExpect(jsonPath("$.validTo").value("9999-12-31"));

        verify(useCase).getBusinessPartnerByCode("BP-042");
    }

    @Test
    void getAllUsesCanonicalCollectionPathAndReturnsFrontendDtoFields() throws Exception {
        BusinessPartner partner = BusinessPartner.reconstitute(
                7L,
                "BP-007",
                "미래은행",
                "987-65-43210",
                "이미래",
                "금융업",
                "은행",
                BusinessPartner.PartnerType.BANK,
                true,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(9999, 12, 31),
                LocalDateTime.of(2025, 12, 1, 9, 0),
                LocalDateTime.of(2026, 1, 1, 9, 0),
                "master-data-admin",
                List.of());
        when(useCase.getAllBusinessPartners()).thenReturn(List.of(partner));

        mockMvc.perform(get("/api/basic/businesspartners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].businessPartnerCode").value("BP-007"))
                .andExpect(jsonPath("$[0].businessPartnerName").value("미래은행"))
                .andExpect(jsonPath("$[0].registrationNumber").value("987-65-*****"))
                .andExpect(jsonPath("$[0].partnerType").value("BANK"))
                .andExpect(jsonPath("$[0].useYn").value(true))
                .andExpect(jsonPath("$[0].kycStatus").value("APPROVED"))
                .andExpect(jsonPath("$[0].riskRating").value("LOW"));

        verify(useCase).getAllBusinessPartners();
    }
}
