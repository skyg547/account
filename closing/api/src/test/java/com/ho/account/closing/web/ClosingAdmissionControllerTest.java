package com.ho.account.closing.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.closing.application.port.in.ClosingAdmissionQuery;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ClosingAdmissionControllerTest {

    private static final LocalDate ACCOUNTING_DATE = LocalDate.of(2026, 9, 25);
    private static final String ADMISSION_PATH = "/api/closing/admission";

    private ClosingAdmissionQuery closingAdmissionQuery;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        closingAdmissionQuery = mock(ClosingAdmissionQuery.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ClosingAdmissionController(closingAdmissionQuery))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modules(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    @ParameterizedTest
    @CsvSource({"false, true", "true, false"})
    void returnsDateAndOrdinaryPostingDecisionWithoutCaching(
            boolean closed, boolean ordinaryPostingAllowed) throws Exception {
        when(closingAdmissionQuery.isClosed(ACCOUNTING_DATE)).thenReturn(closed);

        mockMvc.perform(get(ADMISSION_PATH).param("accountingDate", "2026-09-25"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"accountingDate":"2026-09-25","ordinaryPostingAllowed":%s}
                        """.formatted(ordinaryPostingAllowed), true));

        verify(closingAdmissionQuery).isClosed(ACCOUNTING_DATE);
        verifyNoMoreInteractions(closingAdmissionQuery);
    }

    @Test
    void missingAccountingDateIsRejectedBeforeQuery() throws Exception {
        mockMvc.perform(get(ADMISSION_PATH))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNTING_DATE"));

        verifyNoInteractions(closingAdmissionQuery);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-date", "2026-02-30", "2026-13-01", "2026/09/25", "2026-09-25T10:00:00"})
    void invalidAccountingDateIsRejectedBeforeQuery(String accountingDate) throws Exception {
        mockMvc.perform(get(ADMISSION_PATH).param("accountingDate", accountingDate))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNTING_DATE"));

        verifyNoInteractions(closingAdmissionQuery);
    }

    @ParameterizedTest
    @MethodSource("unavailableQueries")
    void unavailableOrUnknownStatusReturnsSanitizedFailureWithoutCaching(RuntimeException failure)
            throws Exception {
        when(closingAdmissionQuery.isClosed(ACCOUNTING_DATE)).thenThrow(failure);

        mockMvc.perform(get(ADMISSION_PATH).param("accountingDate", "2026-09-25"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().json("""
                        {"code":"CLOSING_ADMISSION_UNAVAILABLE",
                        "message":"Closing admission status is unavailable."}
                        """, true))
                .andExpect(jsonPath("$.ordinaryPostingAllowed").doesNotExist());

        verify(closingAdmissionQuery).isClosed(ACCOUNTING_DATE);
        verifyNoMoreInteractions(closingAdmissionQuery);
    }

    @Test
    void suppliedAdjustmentTypeAndActorCannotOverrideDeniedDate() throws Exception {
        when(closingAdmissionQuery.isClosed(ACCOUNTING_DATE)).thenReturn(true);

        mockMvc.perform(get(ADMISSION_PATH)
                        .param("accountingDate", "2026-09-25")
                        .param("type", "ADJUSTMENT")
                        .param("actor", "SYSTEM")
                        .header("X-Auth-User", "SYSTEM")
                        .header("X-Auth-Roles", "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.ordinaryPostingAllowed").value(false));

        verify(closingAdmissionQuery).isClosed(ACCOUNTING_DATE);
        verifyNoMoreInteractions(closingAdmissionQuery);
    }

    @Test
    void repeatedRequestUsesCurrentDecisionAfterDateIsClosed() throws Exception {
        when(closingAdmissionQuery.isClosed(ACCOUNTING_DATE)).thenReturn(false, true);

        mockMvc.perform(get(ADMISSION_PATH).param("accountingDate", "2026-09-25"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.ordinaryPostingAllowed").value(true));

        mockMvc.perform(get(ADMISSION_PATH).param("accountingDate", "2026-09-25"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.ordinaryPostingAllowed").value(false));

        verify(closingAdmissionQuery, times(2)).isClosed(ACCOUNTING_DATE);
        verifyNoMoreInteractions(closingAdmissionQuery);
    }

    private static Stream<RuntimeException> unavailableQueries() {
        return Stream.of(
                new IllegalStateException("Lookup failed at https://downstream.invalid/status: private body"),
                new NoSuchElementException("Unknown accounting period"),
                new IllegalArgumentException("Unknown downstream status"),
                new RuntimeException("Unexpected persistence failure"));
    }
}
