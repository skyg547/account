package com.ho.account.masterdata.api.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FiscalPeriodControllerTest {

    private FiscalPeriodControlPort fiscalPeriodControlPort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fiscalPeriodControlPort = mock(FiscalPeriodControlPort.class);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new FiscalPeriodController(fiscalPeriodControlPort))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void returnsPeriodContractForJournalValidation() throws Exception {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "08"))
                .thenReturn(Optional.of(new FiscalPeriodRef(
                        8L,
                        "2026",
                        "08",
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        "OPEN")));

        mockMvc.perform(get("/api/basic/fiscal-periods/2026/08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.closingStatus").value("OPEN"));
    }

    @Test
    void returnsNotFoundForUnknownPeriod() throws Exception {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "09"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/basic/fiscal-periods/2026/09"))
                .andExpect(status().isNotFound());
    }

    @Test
    void doesNotExposeRawClosingStatusMutation() throws Exception {
        mockMvc.perform(put("/api/basic/fiscal-periods/id/8/closing-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingStatus\":\"OPEN\"}"))
                .andExpect(status().isNotFound());
    }
}
