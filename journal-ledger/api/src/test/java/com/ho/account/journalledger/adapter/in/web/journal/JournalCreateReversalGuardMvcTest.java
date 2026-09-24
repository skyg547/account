package com.ho.account.journalledger.adapter.in.web.journal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JournalCreateReversalGuardMvcTest {

    private MockMvc mockMvc;
    private JournalPersistencePort journalPersistence;
    private JournalValidationEngine validationEngine;

    @BeforeEach
    void setUp() {
        journalPersistence = org.mockito.Mockito.mock(JournalPersistencePort.class);
        JournalReversalPersistencePort reversals =
                org.mockito.Mockito.mock(JournalReversalPersistencePort.class);
        validationEngine = org.mockito.Mockito.mock(JournalValidationEngine.class);
        JournalEntryService service = new JournalEntryService(
                journalPersistence,
                reversals,
                org.mockito.Mockito.mock(JournalRuleEngine.class),
                org.mockito.Mockito.mock(PostingService.class),
                validationEngine);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new JournalController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVERSAL", " reversal ", "ReVeRsAl"})
    @DisplayName("POST /api/journals는 공백·대소문자를 달리한 caller-supplied REVERSAL을 400으로 거부한다")
    void rejectsCallerSuppliedReversalVariants(String entryType) throws Exception {
        mockMvc.perform(post("/api/journals")
                        .header("X-Auth-User", "journal-maker")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(entryType)))
                .andExpect(status().isBadRequest());

        verify(validationEngine, never()).validate(any());
        verify(journalPersistence, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/journals의 정상 일반 전표 생성은 기존 200 응답을 유지한다")
    void preservesNormalJournalCreation() throws Exception {
        when(journalPersistence.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry saved = invocation.getArgument(0);
            saved.setId(759L);
            saved.setSlipNo("GL759-NORMAL");
            return saved;
        });

        mockMvc.perform(post("/api/journals")
                        .header("X-Auth-User", " Journal-Maker ")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("NORMAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(759L))
                .andExpect(jsonPath("$.entryType").value("NORMAL"))
                .andExpect(jsonPath("$.createdBy").value("journal-maker"))
                .andExpect(jsonPath("$.lines.length()").value(2));

        verify(validationEngine).validate(any(JournalEntry.class));
        verify(journalPersistence).save(any(JournalEntry.class));
    }

    private static String createRequest(String entryType) {
        return """
                {
                  "slipDate":"2026-09-25",
                  "accountingDate":"2026-09-25",
                  "description":"manual journal",
                  "entryType":"%s",
                  "currencyCode":"KRW",
                  "lines":[
                    {"side":"DEBIT","accountCode":"10100","amount":100.00,"baseAmount":100.00},
                    {"side":"CREDIT","accountCode":"40100","amount":100.00,"baseAmount":100.00}
                  ]
                }
                """.formatted(entryType);
    }
}
