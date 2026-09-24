package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceReaggregationControlMigrationTest {

    @Test
    void v15CreatesOneStronglyConstrainedOpenControlRow() {
        PostingTestDatabase database = PostingTestDatabase.create();
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration").schemas(database.schema())
                .target("15").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());

        assertThat(jdbc.queryForObject("SELECT status FROM ledger_reaggregation_control WHERE control_id = 1", String.class))
                .isEqualTo("OPEN");
        assertThat(jdbc.queryForObject("SELECT epoch FROM ledger_reaggregation_control WHERE control_id = 1", Long.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ledger_reaggregation_control", Integer.class)).isOne();

        assertThatThrownBy(() -> jdbc.update("INSERT INTO ledger_reaggregation_control"
                        + " (control_id, status, epoch) VALUES (2, 'OPEN', 0)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE ledger_reaggregation_control"
                        + " SET status = 'REBUILDING', owner_job_instance_id = NULL,"
                        + " range_start = DATE '2026-09-01', range_end = DATE '2026-09-30'"
                        + " WHERE control_id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameOwnerAndRangeIsIdempotentButAnotherOwnerCannotTakeOver() {
        PostingTestDatabase database = PostingTestDatabase.create();
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration").schemas(database.schema())
                .target("15").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        JdbcBalanceReaggregationControlAdapter adapter = new JdbcBalanceReaggregationControlAdapter(jdbc);
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(database.dataSource()));
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        tx.executeWithoutResult(status -> adapter.start(767, start, end));
        tx.executeWithoutResult(status -> adapter.start(767, start, end));

        assertThat(adapter.snapshot().epoch()).isEqualTo(1);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> adapter.start(768, start, end)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ownerJobInstanceId=767");
        tx.executeWithoutResult(status -> adapter.release(767, start, end));
        assertThat(adapter.snapshot().status()).isEqualTo(BalanceReaggregationControlPort.Status.OPEN);
        assertThat(adapter.snapshot().epoch()).isEqualTo(2);
    }
}
