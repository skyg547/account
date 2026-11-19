package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Historical RED probe: copy to the audited a97d10ab core test source, never into the current suite. */
class AuditedPeriodPostingCutoffRegressionTest {
    @Test
    void finalCloseSnapshotCannotExcludeAPostingThatLaterCommits() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:cutoff_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE cutoff_period (id INT PRIMARY KEY, closed BOOLEAN)");
        jdbc.update("INSERT INTO cutoff_period VALUES (1, FALSE)");
        jdbc.execute("CREATE TABLE posting_probe (kind VARCHAR(20) PRIMARY KEY)");
        var journal = approvedJournal();
        var journals = mock(JournalPersistencePort.class);
        var entries = mock(LedgerEntryPersistencePort.class);
        var balances = mock(LedgerService.class);
        when(journals.findByIdWithDetails(1L)).thenReturn(Optional.of(journal));
        when(journals.save(any())).thenAnswer(invocation -> {
            jdbc.update("INSERT INTO posting_probe VALUES ('HEADER')");
            return invocation.getArgument(0);
        });
        doAnswer(invocation -> {
            jdbc.update("INSERT INTO posting_probe VALUES ('GL')");
            jdbc.update("INSERT INTO posting_probe VALUES ('SL')");
            return null;
        }).when(entries).save(any());
        var readyToCommit = new CountDownLatch(1);
        var permitCommit = new CountDownLatch(1);
        doAnswer(invocation -> {
            jdbc.update("INSERT INTO posting_probe VALUES ('BALANCE')");
            readyToCommit.countDown();
            if (!permitCommit.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Synthetic test coordination timed out");
            }
            return null;
        }).when(balances).updateLedgerBalancesBulk(any());
        var service = new PostingService(journals, entries, balances,
                new ClosingLockValidationFilter(date -> jdbc.queryForObject(
                        "SELECT closed FROM cutoff_period WHERE id = 1", Boolean.class)));
        var executor = Executors.newSingleThreadExecutor();
        try {
            var posting = executor.submit(() ->
                    tx.executeWithoutResult(status -> service.postJournalEntry(1L, "poster")));
            assertThat(readyToCommit.await(10, TimeUnit.SECONDS)).isTrue();
            // Real JDBC writes exist, but the real outer transaction has not committed them.
            int snapshot = tx.execute(status -> {
                jdbc.update("UPDATE cutoff_period SET closed = TRUE WHERE id = 1");
                return jdbc.queryForObject("SELECT COUNT(*) FROM posting_probe", Integer.class);
            });
            assertThat(snapshot).isZero();
            permitCommit.countDown();
            posting.get(10, TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT closed FROM cutoff_period WHERE id = 1", Boolean.class)).isTrue();
            // Intentional RED on audited production: expected 0, actual 4 committed effects.
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM posting_probe", Integer.class))
                    .as("A committed final-close snapshot must not miss a later posting")
                    .isEqualTo(snapshot);
        } finally {
            permitCommit.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private JournalEntry approvedJournal() {
        var entry = new JournalEntry();
        entry.setId(1L);
        entry.setSlipNo("SYNTHETIC-CUTOFF");
        entry.setSlipDate(LocalDate.of(2026, 5, 10));
        entry.setAccountingDate(LocalDate.of(2026, 5, 10));
        entry.setCurrencyCode("KRW");
        entry.setLineageSourceType("UNIT_TEST");
        entry.setLineageSourceId("CUTOFF");
        entry.addDetail(detail(11L, JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(12L, JournalSide.CREDIT, "40100"));
        entry.initializeDraft();
        entry.approve("approver");
        return entry;
    }

    private JournalDetail detail(long id, JournalSide side, String account) {
        var detail = new JournalDetail();
        detail.setId(id);
        detail.setSide(side);
        detail.setAccountCode(account);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        return detail;
    }
}
