package com.ho.account.journalledger.adapter.in.web.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JournalForeignCurrencyCreationMvcTest {

    private MockMvc mockMvc;
    private AtomicReference<JournalEntry> receivedEntry;

    @BeforeEach
    void setUp() {
        JournalUseCase journalUseCase = org.mockito.Mockito.mock(JournalUseCase.class);
        receivedEntry = new AtomicReference<>();
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            receivedEntry.set(entry);
            // Standalone MVC still exercises the same aggregate validation used by the real service.
            entry.validateInvariants();
            entry.setId(763L);
            entry.setSlipNo("JE-FX-763");
            entry.initializeDraft();
            return entry;
        });

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new JournalController(journalUseCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("manual USD journal with one-to-one base amount returns 400")
    void rejectsManualForeignJournalWithInconsistentBaseAmount() throws Exception {
        mockMvc.perform(post("/api/journals")
                        .header("X-Auth-User", "manual-fx-maker")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("100.00")))
                .andExpect(status().isBadRequest());

        assertThat(receivedEntry.get()).isNotNull();
        assertThat(receivedEntry.get().getCurrencyCode()).isEqualTo("USD");
        assertThat(receivedEntry.get().getExchangeRate()).isEqualByComparingTo("1300");
    }

    @Test
    @DisplayName("manual USD journal with USD 100 at 1300 and KRW 130000 returns 200")
    void acceptsManualForeignJournalWithConvertedBaseAmount() throws Exception {
        mockMvc.perform(post("/api/journals")
                        .header("X-Auth-User", "manual-fx-maker")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("130000.00")))
                .andExpect(status().isOk());

        assertThat(receivedEntry.get().getDetails()).allSatisfy(detail ->
                assertThat(detail.getBaseAmount()).isEqualByComparingTo("130000.00"));
    }

    private String requestBody(String baseAmount) {
        return """
                {
                  "slipDate": "2026-09-25",
                  "accountingDate": "2026-09-25",
                  "description": "manual foreign journal",
                  "entryType": "NORMAL",
                  "currencyCode": "USD",
                  "exchangeRate": 1300,
                  "lineageSourceType": "MANUAL",
                  "lineageSourceId": "FX-763",
                  "lines": [
                    {"side":"DEBIT","accountCode":"11000","amount":100.00,"baseAmount":%s},
                    {"side":"CREDIT","accountCode":"21000","amount":100.00,"baseAmount":%s}
                  ]
                }
                """.formatted(baseAmount, baseAmount);
    }
}
