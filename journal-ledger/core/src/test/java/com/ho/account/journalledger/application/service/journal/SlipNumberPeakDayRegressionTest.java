package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Compiles against the audited service and exercises its public creation path. */
class SlipNumberPeakDayRegressionTest {
    @Test
    void fiveThousandSameDayCreatesHaveUniqueTwentyCharacterNumbers() {
        AtomicLong next = new AtomicLong();
        List<String> persistedNumbers = new ArrayList<>();
        JournalPersistencePort persistence = mock(JournalPersistencePort.class, invocation -> {
            if (invocation.getMethod().getName().equals("nextSlipNumber")) {
                return next.incrementAndGet();
            }
            if (invocation.getMethod().getName().equals("save")) {
                JournalEntry entry = invocation.getArgument(0);
                persistedNumbers.add(entry.getSlipNo());
                return entry;
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        JournalEntryService service = new JournalEntryService(persistence,
                mock(JournalRuleEngine.class), mock(PostingService.class),
                mock(JournalValidationEngine.class));

        for (int i = 0; i < 5_000; i++) {
            JournalEntry entry = new JournalEntry();
            entry.setSlipDate(LocalDate.of(2026, 10, 9));
            service.createJournalEntry(entry);
        }

        assertThat(persistedNumbers).hasSize(5_000);
        assertThat(new HashSet<>(persistedNumbers)).hasSize(5_000);
        assertThat(persistedNumbers).allSatisfy(number -> assertThat(number).hasSize(20));
    }
}
