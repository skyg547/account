package com.ho.account.closing.application.port.out;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;

/** One consistent provider snapshot, visited twice in V2 canonical ID order. */
public interface AnnualJournalReadPort {
    SourceControl scan(LocalDate startDate, LocalDate endDate,
            BiConsumer<JournalSummary, List<JournalDetailSummary>> firstPass,
            BiConsumer<JournalSummary, List<JournalDetailSummary>> digestPass);

    /** Independent provider aggregate over the same posted, nonannual source rows. */
    record SourceControl(long journalCount, long detailCount, BigDecimal debitBase,
            BigDecimal creditBase, long cutoffJournalId) { }
}
