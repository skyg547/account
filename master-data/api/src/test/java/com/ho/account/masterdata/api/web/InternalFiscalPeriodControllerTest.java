package com.ho.account.masterdata.api.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.api.web.dto.FiscalPeriodStatusUpdateRequestDto;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalFiscalPeriodControllerTest {

    private MockMvc mockMvc;
    private FiscalPeriodControlPort fiscalPeriodControlPort;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        fiscalPeriodControlPort = mock(FiscalPeriodControlPort.class);
        InternalFiscalPeriodController controller = new InternalFiscalPeriodController(fiscalPeriodControlPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void acceptsStatusUpdateWhenAuthorizedServiceIdentityProvided() throws Exception {
        FiscalPeriodRef updatedRef = new FiscalPeriodRef(
                1L, "2026", "03", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), "CLOSED");

        when(fiscalPeriodControlPort.updateClosingStatus(eq(1L), eq("CLOSED"), eq("ADMIN")))
                .thenReturn(updatedRef);

        FiscalPeriodStatusUpdateRequestDto requestDto = new FiscalPeriodStatusUpdateRequestDto("CLOSED", "ADMIN");

        mockMvc.perform(put("/api/internal/fiscal-periods/1/closing-status")
                        .header("X-Service-Identity", "closing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.closingStatus").value("CLOSED"));

        verify(fiscalPeriodControlPort).updateClosingStatus(1L, "CLOSED", "ADMIN");
    }

    @Test
    void rejectsStatusUpdateWhenServiceIdentityMissingOrInvalid() throws Exception {
        FiscalPeriodStatusUpdateRequestDto requestDto = new FiscalPeriodStatusUpdateRequestDto("CLOSED", "ADMIN");

        // 1. 헤더 없음 -> 403 Forbidden
        mockMvc.perform(put("/api/internal/fiscal-periods/1/closing-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        // 2. 잘못된 서비스 헤더 -> 403 Forbidden
        mockMvc.perform(put("/api/internal/fiscal-periods/1/closing-status")
                        .header("X-Service-Identity", "spoofed-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }
}
