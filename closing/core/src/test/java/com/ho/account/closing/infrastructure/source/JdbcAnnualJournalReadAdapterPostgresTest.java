package com.ho.account.closing.infrastructure.source;

import com.ho.account.closing.application.port.out.AnnualJournalReadPort.SourceControl;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in real PostgreSQL MVCC and cursor verification on a disposable localhost database. */
class JdbcAnnualJournalReadAdapterPostgresTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "CLOSING_ANNUAL_TEST_JDBC_URL", matches = ".+")
    void twoPassesAndControlRemainAtOneCutoffThenRestartSeesNewPost() throws SQLException {
        String baseUrl = System.getenv("CLOSING_ANNUAL_TEST_JDBC_URL");
        assumeTrue(baseUrl.startsWith("jdbc:postgresql://127.0.0.1:"));
        String schema = "annual_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource bootstrap = new DriverManagerDataSource(baseUrl, "postgres", "");
        try (Connection connection = bootstrap.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        String schemaUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        DriverManagerDataSource dataSource = new DriverManagerDataSource(schemaUrl, "postgres", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE journal_entries (
                      id bigint PRIMARY KEY, accounting_date date NOT NULL,
                      currency_code varchar(3), slip_date date NOT NULL,
                      slip_no varchar(20) UNIQUE NOT NULL, status varchar(20),
                      entry_type varchar(50), lineage_source_type varchar(50),
                      lineage_source_id varchar(100), description varchar(200))
                    """);
            statement.execute("""
                    CREATE TABLE journal_details (
                      id bigint PRIMARY KEY, journal_entry_id bigint REFERENCES journal_entries(id),
                      side varchar(10), account_code varchar(50), amount numeric(19,2),
                      base_amount numeric(19,2), dept_code varchar(50),
                      business_partner_code varchar(50), detail_description varchar(200))
                    """);
            insertJournal(statement, 1);
            insertJournal(statement, 2);
        }

        JdbcAnnualJournalReadAdapter adapter = new JdbcAnnualJournalReadAdapter(dataSource);
        AtomicInteger firstPass = new AtomicInteger();
        AtomicInteger digestPass = new AtomicInteger();
        SourceControl cutoff = adapter.scan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                (summary, details) -> {
                    firstPass.incrementAndGet();
                    assertThat(details).hasSize(2);
                    if (summary.getId() == 1L) {
                        try (Connection later = dataSource.getConnection();
                                Statement statement = later.createStatement()) {
                            insertJournal(statement, 3); // committed after the cursor snapshot began
                        } catch (SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    }
                }, (summary, details) -> digestPass.incrementAndGet());
        assertThat(firstPass).hasValue(2);
        assertThat(digestPass).hasValue(2);
        assertThat(cutoff).isEqualTo(new SourceControl(2, 4,
                new BigDecimal("2.00"), new BigDecimal("2.00"), 2));

        AtomicInteger restarted = new AtomicInteger();
        SourceControl next = adapter.scan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                (summary, details) -> restarted.incrementAndGet(), (summary, details) -> { });
        assertThat(restarted).hasValue(3);
        assertThat(next.journalCount()).isEqualTo(3);
        assertThat(next.cutoffJournalId()).isEqualTo(3);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            insertJournal(statement, 1_000_000_000L);
        }
        List<Long> canonicalOrder = new ArrayList<>();
        adapter.scan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                (summary, details) -> canonicalOrder.add(summary.getId()),
                (summary, details) -> { });
        assertThat(canonicalOrder).containsExactly(1_000_000_000L, 1L, 2L, 3L);
    }

    private static void insertJournal(Statement statement, long id) throws SQLException {
        statement.execute("INSERT INTO journal_entries VALUES (" + id + ", DATE '2026-06-30', "
                + "'KRW', DATE '2026-06-30', 'SRC-" + id
                + "', 'POSTED', 'NORMAL', NULL, NULL, 'Source " + id + "')");
        statement.execute("INSERT INTO journal_details VALUES (" + (id * 2 - 1) + ", " + id
                + ", 'CREDIT', '41000', 1, 1, NULL, NULL, NULL), (" + (id * 2) + ", " + id
                + ", 'DEBIT', '10000', 1, 1, NULL, NULL, NULL)");
    }
}
