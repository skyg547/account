package com.ho.account.closing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.closing.application.port.in.EodLifecycleUseCase;
import com.ho.account.closing.domain.DailyClosingStatus;
import com.ho.account.closing.domain.EodState;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EodLifecycleControllerTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 30);

    private EodLifecycleUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(EodLifecycleUseCase.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EodLifecycleController(useCase))
                .setControllerAdvice(new EodLifecycleExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modules(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    @Test
    void authorizedAccountingAdminPreparesEodWithTrustedActor() throws Exception {
        DailyClosingStatus prepared = mockStatus(EodState.PRE_CLOSING);
        when(useCase.prepareEod(BUSINESS_DATE, "closer")).thenReturn(prepared);

        mockMvc.perform(post("/api/closing/eod/2026-07-30/eod/prepare")
                        .header(EodLifecycleController.AUTH_USER_HEADER, " closer ")
                        .header(EodLifecycleController.AUTH_ROLES_HEADER, "ROLE_USER,accounting_admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate").value("2026-07-30"))
                .andExpect(jsonPath("$.state").value("PRE_CLOSING"))
                .andExpect(jsonPath("$.transactionAllowed").value(false));

        verify(useCase).prepareEod(BUSINESS_DATE, "closer");
    }

    @Test
    void bodStartMapsBothExplicitBusinessDates() throws Exception {
        LocalDate nextBusinessDate = LocalDate.of(2026, 8, 3);
        DailyClosingStatus bod = mockStatus(EodState.BOD_IN_PROGRESS, nextBusinessDate);
        when(useCase.startBod(BUSINESS_DATE, nextBusinessDate, "closer"))
                .thenReturn(bod);

        mockMvc.perform(post("/api/closing/eod/2026-07-30/bod/2026-08-03/start")
                        .header(EodLifecycleController.AUTH_USER_HEADER, "closer")
                        .header(EodLifecycleController.AUTH_ROLES_HEADER, "ROLE_CLOSING_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate").value("2026-08-03"))
                .andExpect(jsonPath("$.state").value("BOD_IN_PROGRESS"));

        verify(useCase).startBod(BUSINESS_DATE, nextBusinessDate, "closer");
    }

    @Test
    void commandWithoutClosingRoleIsForbiddenBeforeUseCase() throws Exception {
        mockMvc.perform(post("/api/closing/eod/2026-07-30/eod/complete")
                        .header(EodLifecycleController.AUTH_USER_HEADER, "user")
                        .header(EodLifecycleController.AUTH_ROLES_HEADER, "ROLE_USER"))
                .andExpect(status().isForbidden());

        verify(useCase, never()).completeEod(any(), any());
    }

    @Test
    void readWithoutGatewayActorIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/closing/eod/2026-07-30"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    void missingStatusUsesStableNotFoundContract() throws Exception {
        when(useCase.findStatus(BUSINESS_DATE))
                .thenThrow(new NoSuchElementException("No EOD status for 2026-07-30"));

        mockMvc.perform(get("/api/closing/eod/2026-07-30")
                        .header(EodLifecycleController.AUTH_USER_HEADER, "auditor"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EOD_STATUS_NOT_FOUND"));
    }

    private DailyClosingStatus mockStatus(EodState state) {
        return mockStatus(state, BUSINESS_DATE);
    }

    private DailyClosingStatus mockStatus(EodState state, LocalDate businessDate) {
        DailyClosingStatus status = mock(DailyClosingStatus.class);
        when(status.getBusinessDate()).thenReturn(businessDate);
        when(status.getState()).thenReturn(state);
        when(status.getVersion()).thenReturn(0L);
        return status;
    }
}
