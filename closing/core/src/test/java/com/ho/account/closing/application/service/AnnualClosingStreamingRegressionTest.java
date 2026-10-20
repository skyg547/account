package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.AnnualJournalReadPort;
import com.ho.account.closing.domain.ApprovedRetainedEarningsMapping;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/** Regresses full-year list materialization and per-entry Journal/Master calls. */
class AnnualClosingStreamingRegressionTest {
    private static final LocalDate YEAR_END = LocalDate.of(2026, 12, 31);

    @Test
    void streamsTwentyThousandJournalsWithOneProviderCallAndNoPerJournalLookup() {
        SyntheticProvider provider = new SyntheticProvider(20_000, false);
        JournalPostingPort posting = mock(JournalPostingPort.class);
        MasterDataQueryPort master = master();
        service(provider, posting, master).performIncomeStatementClosing(2026);

        assertThat(provider.calls.get()).isOne();
        verify(master).findAccountSubjectAt("35000", YEAR_END);
        verify(master, times(1)).findAccountSubjectAt("41000", LocalDate.of(2026, 6, 30));
        verify(master, times(1)).findAccountSubjectAt("10000", LocalDate.of(2026, 6, 30));
        verify(posting).createDraftEntry(any());
    }

    @Test
    void rejectsProviderControlMismatchBeforeDraftCreation() {
        SyntheticProvider provider = new SyntheticProvider(5, true);
        JournalPostingPort posting = mock(JournalPostingPort.class);

        assertThatThrownBy(() -> service(provider, posting, master())
                .performIncomeStatementClosing(2026))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reconcile");
        verifyNoInteractions(posting);
    }

    @Test
    void rejectsUnboundedDistinctDatedClassificationsBeforePosting() {
        SyntheticProvider provider = new SyntheticProvider(10_001, false, true);
        JournalPostingPort posting = mock(JournalPostingPort.class);

        assertThatThrownBy(() -> service(provider, posting, master())
                .performIncomeStatementClosing(2026))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bounded review limit");
        verifyNoInteractions(posting);
    }

    private static AnnualClosingService service(AnnualJournalReadPort provider,
            JournalPostingPort posting, MasterDataQueryPort master) {
        return new AnnualClosingService(posting, master,
                year -> new ApprovedRetainedEarningsMapping(
                        "ENTITY-01", year, "35000", true, "checker", "CHG-888"),
                provider);
    }

    private static MasterDataQueryPort master() {
        MasterDataQueryPort master = mock(MasterDataQueryPort.class);
        when(master.findAccountSubjectAt("35000", YEAR_END)).thenReturn(Optional.of(
                new AccountSubjectRef("35000", "Retained", false, false, "CREDIT", "EQUITY")));
        when(master.findAccountSubjectAt("41000", LocalDate.of(2026, 6, 30))).thenReturn(Optional.of(
                new AccountSubjectRef("41000", "Revenue", false, false, "CREDIT", "REVENUE")));
        when(master.findAccountSubjectAt("10000", LocalDate.of(2026, 6, 30))).thenReturn(Optional.of(
                new AccountSubjectRef("10000", "Cash", false, false, "DEBIT", "ASSETS")));
        return master;
    }

    private static final class SyntheticProvider implements AnnualJournalReadPort {
        private final int journalCount;
        private final boolean mismatch;
        private final boolean distinctAccounts;
        private final AtomicInteger calls = new AtomicInteger();

        private SyntheticProvider(int journalCount, boolean mismatch) {
            this(journalCount, mismatch, false);
        }

        private SyntheticProvider(int journalCount, boolean mismatch, boolean distinctAccounts) {
            this.journalCount = journalCount;
            this.mismatch = mismatch;
            this.distinctAccounts = distinctAccounts;
        }

        @Override
        public SourceControl scan(LocalDate startDate, LocalDate endDate,
                BiConsumer<JournalSummary, List<JournalDetailSummary>> firstPass,
                BiConsumer<JournalSummary, List<JournalDetailSummary>> digestPass) {
            calls.incrementAndGet();
            visit(firstPass);
            visit(digestPass);
            return new SourceControl(journalCount, journalCount * 2L, BigDecimal.valueOf(journalCount),
                    BigDecimal.valueOf(journalCount - (mismatch ? 1 : 0)), journalCount);
        }

        private void visit(BiConsumer<JournalSummary, List<JournalDetailSummary>> consumer) {
            // Same ordering as the PostgreSQL provider's V2 canonical-ID ordering.
            for (int digits = 1; digits <= Integer.toString(journalCount).length(); digits++) {
                int first = digits == 1 ? 1 : (int) Math.pow(10, digits - 1);
                int last = Math.min(journalCount, (int) Math.pow(10, digits) - 1);
                for (int id = first; id <= last; id++) {
                    JournalSummary header = new JournalSummary();
                    header.setId((long) id);
                    header.setSlipNo("SRC-" + id);
                    header.setSlipDate(LocalDate.of(2026, 6, 30));
                    header.setAccountingDate(LocalDate.of(2026, 6, 30));
                    header.setDescription("Source " + id);
                    header.setStatus("POSTED");
                    header.setEntryType("NORMAL");
                    header.setCurrencyCode("KRW");
                    JournalDetailSummary detail = new JournalDetailSummary();
                    detail.setId(id * 2L - 1);
                    detail.setSlipNo(header.getSlipNo());
                    detail.setHeaderDescription(header.getDescription());
                    detail.setAccountingDate(header.getAccountingDate());
                    detail.setAccountCode(distinctAccounts ? "R" + id : "41000");
                    detail.setAccountCategory(distinctAccounts ? "REVENUE" : null);
                    detail.setSide(JournalSide.CREDIT);
                    detail.setAmount(BigDecimal.ONE);
                    detail.setBaseAmount(BigDecimal.ONE);
                    JournalDetailSummary offset = new JournalDetailSummary();
                    offset.setId(id * 2L);
                    offset.setSlipNo(header.getSlipNo());
                    offset.setHeaderDescription(header.getDescription());
                    offset.setAccountingDate(header.getAccountingDate());
                    offset.setAccountCode("10000");
                    offset.setSide(JournalSide.DEBIT);
                    offset.setAmount(BigDecimal.ONE);
                    offset.setBaseAmount(BigDecimal.ONE);
                    consumer.accept(header, List.of(detail, offset));
                }
            }
        }
    }
}
