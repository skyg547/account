package com.ho.account.closing.infrastructure.source;

import com.ho.account.closing.application.port.out.AnnualJournalReadPort;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** PostgreSQL cursor and independent control totals share one repeatable-read MVCC snapshot. */
public final class JdbcAnnualJournalReadAdapter implements AnnualJournalReadPort {
    private static final int FETCH_SIZE = 1_000;
    private static final int MAX_LINES_PER_JOURNAL = 10_000;
    private static final int QUERY_TIMEOUT_SECONDS = 60;
    private static final String SOURCE = "e.status = 'POSTED' AND "
            + "COALESCE(e.lineage_source_type, '') <> 'ANNUAL_CLOSING' "
            + "AND e.slip_no NOT LIKE 'ACL%'";
    private static final String INCLUDED = "(" + SOURCE + " OR "
            + "e.lineage_source_type = 'ANNUAL_CLOSING' OR e.slip_no LIKE 'ACL%')";
    public static final String STREAM_SQL = """
            SELECT e.id, e.slip_no, e.slip_date, e.accounting_date, e.description,
                   e.status, e.entry_type, e.currency_code, e.lineage_source_type,
                   e.lineage_source_id, d.id AS detail_id, d.side, d.account_code,
                   d.amount, d.base_amount, d.dept_code, d.business_partner_code,
                   d.detail_description
              FROM journal_entries e
              LEFT JOIN journal_details d ON d.journal_entry_id = e.id
             WHERE e.accounting_date BETWEEN ? AND ? AND %s
             ORDER BY (LENGTH(e.id::text)::text || ':' || e.id::text) COLLATE "C",
                      (LENGTH(d.id::text)::text || ':' || d.id::text) COLLATE "C"
            """.formatted(INCLUDED);
    public static final String CONTROL_SQL = """
            SELECT COUNT(DISTINCT e.id) AS journal_count,
                   COUNT(d.id) AS detail_count,
                   COALESCE(SUM(CASE WHEN d.side = 'DEBIT' THEN d.base_amount ELSE 0 END), 0) AS debit_base,
                   COALESCE(SUM(CASE WHEN d.side = 'CREDIT' THEN d.base_amount ELSE 0 END), 0) AS credit_base,
                   COALESCE(MAX(e.id), 0) AS cutoff_journal_id
              FROM journal_entries e
              LEFT JOIN journal_details d ON d.journal_entry_id = e.id
             WHERE e.accounting_date BETWEEN ? AND ? AND %s
            """.formatted(SOURCE);

    private final DataSource dataSource;

    public JdbcAnnualJournalReadAdapter(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public SourceControl scan(LocalDate startDate, LocalDate endDate,
            BiConsumer<JournalSummary, List<JournalDetailSummary>> firstPass,
            BiConsumer<JournalSummary, List<JournalDetailSummary>> digestPass) {
        Objects.requireNonNull(startDate);
        Objects.requireNonNull(endDate);
        Objects.requireNonNull(firstPass);
        Objects.requireNonNull(digestPass);
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("annual date range is reversed");
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false); // PostgreSQL fetch-size cursor needs a transaction.
            try {
                stream(connection, startDate, endDate, firstPass);
                stream(connection, startDate, endDate, digestPass);
                SourceControl control = control(connection, startDate, endDate);
                connection.commit();
                return control;
            } catch (RuntimeException | SQLException failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("annual journal snapshot query failed", failure);
        }
    }

    private void stream(Connection connection, LocalDate startDate, LocalDate endDate,
            BiConsumer<JournalSummary, List<JournalDetailSummary>> consumer) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(STREAM_SQL)) {
            bindDates(statement, startDate, endDate);
            statement.setFetchSize(FETCH_SIZE);
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rows = statement.executeQuery()) {
                JournalSummary current = null;
                List<JournalDetailSummary> details = new ArrayList<>();
                while (rows.next()) {
                    long id = rows.getLong("id");
                    if (current == null || !current.getId().equals(id)) {
                        if (current != null) {
                            consumer.accept(current, List.copyOf(details));
                            details.clear();
                        }
                        current = summary(rows);
                    }
                    Long detailId = rows.getObject("detail_id", Long.class);
                    if (detailId != null) {
                        if (details.size() == MAX_LINES_PER_JOURNAL) {
                            throw new IllegalStateException("annual source journal exceeds bounded line limit");
                        }
                        details.add(detail(rows, current, detailId));
                    }
                }
                if (current != null) {
                    consumer.accept(current, List.copyOf(details));
                }
            }
        }
    }

    private SourceControl control(Connection connection, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CONTROL_SQL)) {
            bindDates(statement, startDate, endDate);
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new IllegalStateException("annual source control query returned no row");
                }
                SourceControl control = new SourceControl(rows.getLong("journal_count"),
                        rows.getLong("detail_count"), rows.getBigDecimal("debit_base"),
                        rows.getBigDecimal("credit_base"), rows.getLong("cutoff_journal_id"));
                if (rows.next()) {
                    throw new IllegalStateException("annual source control query returned multiple rows");
                }
                return control;
            }
        }
    }

    private static void bindDates(PreparedStatement statement, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        statement.setDate(1, Date.valueOf(startDate));
        statement.setDate(2, Date.valueOf(endDate));
    }

    private static JournalSummary summary(ResultSet rows) throws SQLException {
        JournalSummary summary = new JournalSummary();
        summary.setId(rows.getLong("id"));
        summary.setSlipNo(rows.getString("slip_no"));
        summary.setSlipDate(rows.getDate("slip_date").toLocalDate());
        summary.setAccountingDate(rows.getDate("accounting_date").toLocalDate());
        summary.setDescription(rows.getString("description"));
        summary.setStatus(rows.getString("status"));
        summary.setEntryType(rows.getString("entry_type"));
        summary.setCurrencyCode(rows.getString("currency_code"));
        summary.setLineageSourceType(rows.getString("lineage_source_type"));
        summary.setLineageSourceId(rows.getString("lineage_source_id"));
        return summary;
    }

    private static JournalDetailSummary detail(ResultSet rows, JournalSummary header, Long detailId)
            throws SQLException {
        JournalDetailSummary detail = new JournalDetailSummary();
        detail.setId(detailId);
        detail.setSide(JournalSide.valueOf(rows.getString("side")));
        detail.setAccountCode(rows.getString("account_code"));
        detail.setAmount(rows.getBigDecimal("amount"));
        detail.setBaseAmount(rows.getBigDecimal("base_amount"));
        detail.setDepartmentCode(rows.getString("dept_code"));
        detail.setBusinessPartnerCode(rows.getString("business_partner_code"));
        detail.setDetailDescription(rows.getString("detail_description"));
        detail.setAccountingDate(header.getAccountingDate());
        detail.setSlipNo(header.getSlipNo());
        detail.setHeaderDescription(header.getDescription());
        return detail;
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("dev")
    @ConditionalOnProperty(name = "closing.sources.enabled", havingValue = "true")
    public static class Wiring {
        @Bean
        public AnnualJournalReadPort annualJournalReadPort(ClosingReadOnlySources sources) {
            return new JdbcAnnualJournalReadAdapter(sources.journalDataSource());
        }
    }
}
