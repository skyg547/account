package com.ho.account.journalledger.adapter.in.web.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JournalAuditActorSanitizationMvcTest {

    private static final LocalDate ACCOUNTING_DATE = LocalDate.of(2026, 9, 25);
    private static final String TRUSTED_MAKER = "trusted-maker";

    private final AtomicReference<Map<String, Object>> receivedEvent = new AtomicReference<>();
    private final AtomicReference<JournalEntryCommand> receivedCommand = new AtomicReference<>();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JournalUseCase journalUseCase = mock(JournalUseCase.class);
        JournalPostingPort journalPostingPort = mock(JournalPostingPort.class);

        when(journalUseCase.createJournalEntryFromEvent(anyMap(), eq(ACCOUNTING_DATE)))
                .thenAnswer(invocation -> {
                    receivedEvent.set(new HashMap<>(invocation.getArgument(0)));
                    return Optional.empty();
                });
        when(journalPostingPort.createDraftEntry(any(JournalEntryCommand.class)))
                .thenAnswer(invocation -> {
                    receivedCommand.set(invocation.getArgument(0));
                    return new JournalPostingResult(757L, "JL-757", "DRAFT");
                });

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new JournalController(journalUseCase),
                        new JournalPostingRestController(journalPostingPort))
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("untrustedBodyActors")
    @DisplayName("HTTP 이벤트는 body actor 대신 trusted caller를 사용한다")
    void replacesBodyActorsAtEventWebBoundary(
            String scenario, String bodyCreatedBy, String bodyAuditUser) throws Exception {
        // Both header generations keep this regression executable against the audited and current boundaries.
        mockMvc.perform(post("/api/journals/from-event")
                        .queryParam("accountingDate", ACCOUNTING_DATE.toString())
                        .header("X-User-ID", TRUSTED_MAKER)
                        .header("X-Auth-User", TRUSTED_MAKER)
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(bodyCreatedBy, bodyAuditUser)))
                .andExpect(status().isNoContent());

        // These fields establish approval lineage, so request JSON can never outrank trusted inbound context.
        assertThat(receivedEvent.get())
                .containsEntry("createdBy", TRUSTED_MAKER)
                .containsEntry("auditUser", TRUSTED_MAKER);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("untrustedBodyActors")
    @DisplayName("posting 계약은 body actor 대신 trusted caller를 사용한다")
    void replacesBodyActorsAtPostingContractWebBoundary(
            String scenario, String bodyCreatedBy, String bodyAuditUser) throws Exception {
        mockMvc.perform(post("/api/v1/journals/posting")
                        .header("X-User-ID", TRUSTED_MAKER)
                        .header("X-Auth-User", TRUSTED_MAKER)
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postingBody(bodyCreatedBy, bodyAuditUser)))
                .andExpect(status().isOk());

        assertThat(receivedCommand.get().createdBy()).isEqualTo(TRUSTED_MAKER);
        assertThat(receivedCommand.get().auditUser()).isEqualTo(TRUSTED_MAKER);
    }

    private static Stream<Arguments> untrustedBodyActors() {
        return Stream.of(
                Arguments.of("forged actors", "payload-attacker", "payload-attacker"),
                Arguments.of("blank actors", " ", ""),
                Arguments.of("conflicting actors", "payload-maker", "payload-checker"));
    }

    private static String eventBody(String createdBy, String auditUser) {
        return """
                {"eventData":{"ruleCode":"AUDIT_ACTOR_RULE","amount":100.00,
                  "createdBy":"%s","auditUser":"%s"}}
                """.formatted(createdBy, auditUser);
    }

    private static String postingBody(String createdBy, String auditUser) {
        return """
                {"slipDate":"2026-09-25","accountingDate":"2026-09-25",
                 "description":"trusted actor regression","entryType":"GENERAL",
                 "currencyCode":"KRW","exchangeRate":1.00,
                 "createdBy":"%s","auditUser":"%s",
                 "lineageSourceType":"ISSUE_757","lineageSourceId":"ACTOR-757",
                 "lines":[
                   {"drcrType":"DEBIT","accountCode":"10100","amount":100.00,"baseAmount":100.00},
                   {"drcrType":"CREDIT","accountCode":"20100","amount":100.00,"baseAmount":100.00}
                 ]}
                """.formatted(createdBy, auditUser);
    }
}
