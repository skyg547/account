package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/** JDBC adapter kept independent of the selected JPA/JDBC-bulk balance writer. */
@Component
@RequiredArgsConstructor
public class JdbcBalanceReaggregationControlAdapter implements BalanceReaggregationControlPort {

    private static final String CONTROL_COLUMNS =
            "status, owner_job_instance_id, range_start, range_end, epoch";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ControlSnapshot snapshot() {
        return readControl(false);
    }

    @Override
    public ControlSnapshot start(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate) {
        requireRange(startDate, endDate);
        ControlSnapshot current = readControl(true);
        if (!current.isOpen()) {
            if (current.ownerJobInstanceId() != null
                    && current.ownerJobInstanceId() == ownerJobInstanceId
                    && startDate.equals(current.startDate()) && endDate.equals(current.endDate())) {
                return current;
            }
            throw conflict(current);
        }
        int changed = jdbcTemplate.update("UPDATE ledger_reaggregation_control"
                        + " SET status = 'REBUILDING', owner_job_instance_id = ?, range_start = ?,"
                        + " range_end = ?, epoch = epoch + 1 WHERE control_id = 1 AND status = 'OPEN'",
                ownerJobInstanceId, startDate, endDate);
        if (changed != 1) {
            throw new IllegalStateException("Balance reaggregation control row changed unexpectedly");
        }
        return new ControlSnapshot(Status.REBUILDING, ownerJobInstanceId, startDate, endDate, current.epoch() + 1);
    }

    @Override
    public void assertOpen() {
        ControlSnapshot current = snapshot();
        if (!current.isOpen()) {
            throw conflict(current);
        }
    }

    @Override
    public void assertOwner(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate) {
        ControlSnapshot current = readControl(true);
        if (current.status() != Status.REBUILDING
                || current.ownerJobInstanceId() == null
                || current.ownerJobInstanceId() != ownerJobInstanceId
                || !startDate.equals(current.startDate()) || !endDate.equals(current.endDate())) {
            throw conflict(current);
        }
    }

    @Override
    public ReconciliationData loadReconciliationData(LocalDate startDate, LocalDate endDate) {
        List<GlMovement> glMovements = jdbcTemplate.query("""
                SELECT je.accounting_date, jd.account_code, je.currency_code,
                       SUM(CASE WHEN jd.side = 'DEBIT' THEN jd.base_amount ELSE 0 END),
                       SUM(CASE WHEN jd.side = 'CREDIT' THEN jd.base_amount ELSE 0 END)
                  FROM journal_details jd JOIN journal_entries je ON je.id = jd.journal_entry_id
                 WHERE je.status = 'POSTED' AND je.accounting_date BETWEEN ? AND ?
                 GROUP BY je.accounting_date, jd.account_code, je.currency_code
                 ORDER BY je.accounting_date, jd.account_code, je.currency_code
                """, (rs, row) -> new GlMovement(rs.getObject(1, LocalDate.class),
                new GlKey(rs.getString(2), rs.getString(3)), rs.getBigDecimal(4), rs.getBigDecimal(5)),
                startDate, endDate);
        List<SlMovement> slMovements = jdbcTemplate.query("""
                SELECT je.accounting_date, jd.account_code, jd.business_partner_code, jd.dept_code,
                       je.currency_code,
                       SUM(CASE WHEN jd.side = 'DEBIT' THEN jd.base_amount ELSE 0 END),
                       SUM(CASE WHEN jd.side = 'CREDIT' THEN jd.base_amount ELSE 0 END)
                  FROM journal_details jd JOIN journal_entries je ON je.id = jd.journal_entry_id
                 WHERE je.status = 'POSTED' AND je.accounting_date BETWEEN ? AND ?
                 GROUP BY je.accounting_date, jd.account_code, jd.business_partner_code, jd.dept_code, je.currency_code
                 ORDER BY je.accounting_date, jd.account_code, jd.business_partner_code, jd.dept_code, je.currency_code
                """, (rs, row) -> new SlMovement(rs.getObject(1, LocalDate.class),
                new SlKey(rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                rs.getBigDecimal(6), rs.getBigDecimal(7)), startDate, endDate);
        List<GlActual> glActuals = jdbcTemplate.query("""
                SELECT balance_date, account_code, currency_code, beginning_balance,
                       debit_amount, credit_amount, ending_balance
                  FROM gl_balances WHERE balance_date BETWEEN ? AND ?
                """, (rs, row) -> new GlActual(rs.getObject(1, LocalDate.class),
                new GlKey(rs.getString(2), rs.getString(3)), rs.getBigDecimal(4),
                rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7)), startDate, endDate);
        List<SlActual> slActuals = jdbcTemplate.query("""
                SELECT balance_date, account_code, bp_code, dept_code, currency_code,
                       beginning_balance, debit_amount, credit_amount, ending_balance
                  FROM sl_balances WHERE balance_date BETWEEN ? AND ?
                """, (rs, row) -> new SlActual(rs.getObject(1, LocalDate.class),
                new SlKey(rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9)),
                startDate, endDate);
        List<GlPrior> glPriors = jdbcTemplate.query("""
                SELECT account_code, currency_code, ending_balance FROM (
                    SELECT account_code, currency_code, ending_balance,
                           ROW_NUMBER() OVER (PARTITION BY account_code, currency_code ORDER BY balance_date DESC) AS rn
                      FROM gl_balances WHERE balance_date < ?
                ) prior WHERE rn = 1
                """, (rs, row) -> new GlPrior(new GlKey(rs.getString(1), rs.getString(2)), rs.getBigDecimal(3)), startDate);
        List<SlPrior> slPriors = jdbcTemplate.query("""
                SELECT account_code, bp_code, dept_code, currency_code, ending_balance FROM (
                    SELECT account_code, bp_code, dept_code, currency_code, ending_balance,
                           ROW_NUMBER() OVER (PARTITION BY account_code, bp_code, dept_code, currency_code
                                              ORDER BY balance_date DESC) AS rn
                      FROM sl_balances WHERE balance_date < ?
                ) prior WHERE rn = 1
                """, (rs, row) -> new SlPrior(new SlKey(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                rs.getBigDecimal(5)), startDate);
        return new ReconciliationData(glMovements, slMovements, glActuals, slActuals, glPriors, slPriors);
    }

    @Override
    public void release(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate) {
        assertOwner(ownerJobInstanceId, startDate, endDate);
        int changed = jdbcTemplate.update("UPDATE ledger_reaggregation_control"
                + " SET status = 'OPEN', owner_job_instance_id = NULL, range_start = NULL, range_end = NULL,"
                + " epoch = epoch + 1 WHERE control_id = 1 AND status = 'REBUILDING' AND owner_job_instance_id = ?",
                ownerJobInstanceId);
        if (changed != 1) {
            throw new IllegalStateException("Balance reaggregation control release lost ownership");
        }
    }

    private ControlSnapshot readControl(boolean forUpdate) {
        String sql = "SELECT " + CONTROL_COLUMNS + " FROM ledger_reaggregation_control WHERE control_id = 1"
                + (forUpdate ? " FOR UPDATE" : "");
        List<ControlSnapshot> rows = jdbcTemplate.query(sql, (rs, row) -> new ControlSnapshot(
                Status.valueOf(rs.getString(1)), nullableLong(rs.getObject(2)),
                nullableDate(rs.getDate(3)), nullableDate(rs.getDate(4)), rs.getLong(5)));
        if (rows.size() != 1) {
            throw new IllegalStateException("Ledger reaggregation control singleton is missing; apply migration V15");
        }
        return rows.get(0);
    }

    private Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private LocalDate nullableDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private void requireRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("A valid reaggregation date range is required");
        }
    }

    private IllegalStateException conflict(ControlSnapshot current) {
        return new IllegalStateException("Ledger balance reaggregation is not OPEN; status=" + current.status()
                + ", ownerJobInstanceId=" + current.ownerJobInstanceId()
                + ", range=" + current.startDate() + ".." + current.endDate());
    }
}
