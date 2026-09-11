package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JournalByIdQueryTest {
    @Test
    void returnsLineageAndFinancialLinesFromDetailedUseCase() throws Exception {
        JournalUseCase useCase = mock(JournalUseCase.class);
        JournalEntry entry = new JournalEntry();
        entry.setId(42L);
        entry.setSlipNo("CLOSE-42");
        entry.setLineageSourceType("FX_VALUATION");
        entry.setLineageSourceId("690|10100|USD");
        JournalDetail detail = new JournalDetail();
        detail.setId(43L);
        detail.setSide(JournalSide.DEBIT);
        detail.setAccountCode("10100");
        detail.setAmount(new BigDecimal("1200.01"));
        detail.setBaseAmount(new BigDecimal("1200.01"));
        detail.setDetailDescription("FX adjustment");
        entry.addDetail(detail);
        when(useCase.getJournalEntryWithDetails(42L)).thenReturn(Optional.of(entry));
        MockMvcBuilders.standaloneSetup(new JournalController(useCase)).build()
                .perform(get("/api/journals/by-id/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineageSourceId").value("690|10100|USD"))
                .andExpect(jsonPath("$.lines[0].baseAmount").value(1200.01))
                .andExpect(jsonPath("$.lines[0].side").value("DEBIT"));
        verify(useCase).getJournalEntryWithDetails(42L);
    }

    @Test
    void missingAndInvalidIdsDoNotReturnFabricatedJournal() throws Exception {
        JournalUseCase useCase = mock(JournalUseCase.class);
        when(useCase.getJournalEntryWithDetails(42L)).thenReturn(Optional.empty());
        var mvc = MockMvcBuilders.standaloneSetup(new JournalController(useCase)).build();
        mvc.perform(get("/api/journals/by-id/42")).andExpect(status().isNotFound());
        mvc.perform(get("/api/journals/by-id/0")).andExpect(status().isBadRequest());
        verify(useCase, never()).getJournalEntryWithDetails(0L);
    }
}
