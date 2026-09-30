package com.ho.account.journalledger.adapter.in.web.journal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import com.ho.account.journalledger.application.service.journal.JournalEventQuarantineNotFoundException;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantineStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JournalEventQuarantineControllerTest {

    private KafkaJournalEventUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = Mockito.mock(KafkaJournalEventUseCase.class);
        ObjectMapper productionTimeMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new JournalEventQuarantineController(useCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(productionTimeMapper))
                .build();
    }

    @Test
    void completenessSummaryRequiresTrustedAccountingAdmin() throws Exception {
        mockMvc.perform(get("/api/journals/event-quarantine/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void completenessSummaryExposesCountsAndAgeWithoutPayloadOrKafkaKey() throws Exception {
        when(useCase.completenessSummary()).thenReturn(
                new KafkaJournalEventUseCase.CompletenessSummary(
                        3L, 7L, Optional.of(Instant.parse("2026-09-28T01:02:03Z"))));

        mockMvc.perform(get("/api/journals/event-quarantine/summary")
                        .header("X-Auth-User", "control.operator")
                        .header("X-Auth-Roles", "ROLE_ACCOUNTING_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quarantinedCount").value(3))
                .andExpect(jsonPath("$.replayedCount").value(7))
                .andExpect(jsonPath("$.oldestUnresolvedAt").value("2026-09-28T01:02:03Z"))
                .andExpect(jsonPath("$.payloadJson").doesNotExist())
                .andExpect(jsonPath("$.sourceKey").doesNotExist());
    }

    @Test
    void invalidListLimitReturns400WithoutCallingPersistenceUseCase() throws Exception {
        mockMvc.perform(get("/api/journals/event-quarantine")
                        .header("X-Auth-User", "control.operator")
                        .header("X-Auth-Roles", "ROLE_ACCOUNTING_ADMIN")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());

        verify(useCase, never()).find(any(), any(Integer.class));
    }

    @Test
    void authorizedListReturnsMetadataWithoutStoredPayload() throws Exception {
        JournalEventQuarantine quarantine = unmatched();
        when(useCase.find(JournalEventQuarantineStatus.QUARANTINED, 1))
                .thenReturn(List.of(quarantine));

        mockMvc.perform(get("/api/journals/event-quarantine")
                        .header("X-Auth-User", "control.operator")
                        .header("X-Auth-Roles", "ROLE_ACCOUNTING_ADMIN")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceTopic").value("transaction-events"))
                .andExpect(jsonPath("$[0].sourcePartition").value(2))
                .andExpect(jsonPath("$[0].sourceOffset").value(769))
                .andExpect(jsonPath("$[0].payloadJson").doesNotExist());
    }

    @Test
    void missingReplayTargetReturns404() throws Exception {
        when(useCase.replay(404L, "control.operator"))
                .thenThrow(new JournalEventQuarantineNotFoundException(404L));

        mockMvc.perform(post("/api/journals/event-quarantine/404/replay")
                        .header("X-Auth-User", "control.operator")
                        .header("X-Auth-Roles", "ROLE_ACCOUNTING_ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void replayPersistenceFailureIsNotCollapsedToNotFound() {
        when(useCase.replay(18L, "control.operator"))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> mockMvc.perform(post("/api/journals/event-quarantine/18/replay")
                        .header("X-Auth-User", "control.operator")
                        .header("X-Auth-Roles", "ROLE_ACCOUNTING_ADMIN")))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("database unavailable");
    }

    @Test
    void successfulReplayReturnsLinkedJournalAndDisposition() throws Exception {
        JournalEventQuarantine quarantine = unmatched();
        quarantine.noteReplayAttempt("control.operator", Instant.parse("2026-09-28T02:00:00Z"));
        quarantine.markReplayed(99L);
        JournalEntry journal = new JournalEntry();
        journal.setId(99L);
        when(useCase.replay(18L, "control.operator")).thenReturn(
                new KafkaJournalEventUseCase.ReplayResult(true, true, journal, quarantine));

        mockMvc.perform(post("/api/journals/event-quarantine/18/replay")
                        .header("X-Auth-User", "control.operator")
                        .header("X-Auth-Roles", "ROLE_ACCOUNTING_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.newlyCreated").value(true))
                .andExpect(jsonPath("$.journalEntryId").value(99));
    }

    private JournalEventQuarantine unmatched() {
        return JournalEventQuarantine.unmatched(
                "transaction-events",
                2,
                769L,
                "{\"ruleCode\":\"NO_RULE\"}",
                LocalDate.of(2026, 9, 28),
                Instant.parse("2026-09-28T01:02:03Z"));
    }
}
