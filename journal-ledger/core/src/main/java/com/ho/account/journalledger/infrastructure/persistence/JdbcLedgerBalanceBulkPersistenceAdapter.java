package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;

import java.math.BigDecimal;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 원장 잔액을 JDBC bulk upsert로 저장하는 운영용 출력 어댑터입니다.
 *
 * <p>초보자 관점에서 "upsert"는 같은 잔액 키가 없으면 insert, 있으면 update 하는 저장 방식입니다.
 * 재전기나 과거 기간 재집계에서는 같은 날짜·계정·통화 키를 다시 계산할 수 있으므로,
 * 애플리케이션 루프에서 한 건씩 저장하지 않고 DB에 일괄 반영합니다.</p>
 */
@Component
@ConditionalOnProperty(name = "journal-ledger.ledger.persistence-mode", havingValue = "jdbc-bulk")
@RequiredArgsConstructor
public class JdbcLedgerBalanceBulkPersistenceAdapter implements LedgerBalancePersistencePort {

    private static final String GL_COLUMNS = """
            account_code, currency_code, balance_date, period, beginning_balance,
            debit_amount, credit_amount, ending_balance, created_at, updated_at
            """;

    private static final String SL_COLUMNS = """
            account_code, bp_code, dept_code, currency_code, balance_date, period,
            beginning_balance, debit_amount, credit_amount, ending_balance, created_at, updated_at
            """;

    private static final String INSERT_SL_BALANCE = "INSERT INTO sl_balances (" + SL_COLUMNS + ") "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SL_BALANCE = """
            UPDATE sl_balances
               SET beginning_balance = ?,
                   debit_amount = ?,
                   credit_amount = ?,
                   ending_balance = ?,
                   updated_at = ?
             WHERE account_code = ?
               AND ((? IS NULL AND bp_code IS NULL) OR bp_code = ?)
               AND ((? IS NULL AND dept_code IS NULL) OR dept_code = ?)
               AND currency_code = ?
               AND balance_date = ?
               AND period = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final GlBalanceRepository glBalanceRepository;
    private final SlBalanceRepository slBalanceRepository;
    private final JournalDetailRepository journalDetailRepository;

    @Override
    public Optional<GlBalance> findGlBalance(
            String accountCode, String currencyCode, LocalDate balanceDate, YearMonth period) {
        return glBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                accountCode, currencyCode, balanceDate, period);
    }

    @Override
    public Optional<GlBalance> findPreviousGlBalance(
            String accountCode, String currencyCode, LocalDate balanceDate) {
        return glBalanceRepository.findFirstByAccountCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
                accountCode, currencyCode, balanceDate);
    }

    @Override
    public GlBalance saveGlBalance(GlBalance balance) {
        saveGlBalances(List.of(balance));
        return balance;
    }

    @Override
    public void saveGlBalances(List<GlBalance> balances) {
        if (balances == null || balances.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.batchUpdate(glUpsertSql(), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                bindGlBalance(ps, balances.get(i), now);
            }

            @Override
            public int getBatchSize() {
                return balances.size();
            }
        });
    }

    @Override
    public Optional<SlBalance> findSlBalance(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate, YearMonth period) {
        return slBalanceRepository.findByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                accountCode, businessPartnerCode, departmentCode, currencyCode, balanceDate, period);
    }

    @Override
    public Optional<SlBalance> findPreviousSlBalance(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate) {
        return slBalanceRepository.findFirstByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
                accountCode, businessPartnerCode, departmentCode, currencyCode, balanceDate);
    }

    @Override
    public SlBalance saveSlBalance(SlBalance balance) {
        saveSlBalances(List.of(balance));
        return balance;
    }

    @Override
    public void saveSlBalances(List<SlBalance> balances) {
        if (balances == null || balances.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int[] updatedCounts = jdbcTemplate.batchUpdate(UPDATE_SL_BALANCE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                bindSlBalanceUpdate(ps, balances.get(i), now);
            }

            @Override
            public int getBatchSize() {
                return balances.size();
            }
        });

        List<SlBalance> inserts = new java.util.ArrayList<>();
        for (int i = 0; i < updatedCounts.length; i++) {
            if (updatedCounts[i] == 0) {
                inserts.add(balances.get(i));
            }
        }
        if (inserts.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(INSERT_SL_BALANCE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                bindSlBalanceInsert(ps, inserts.get(i), now);
            }

            @Override
            public int getBatchSize() {
                return inserts.size();
            }
        });
    }

    @Override
    public void deleteBalancesBetween(LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update("DELETE FROM gl_balances WHERE balance_date BETWEEN ? AND ?", startDate, endDate);
        jdbcTemplate.update("DELETE FROM sl_balances WHERE balance_date BETWEEN ? AND ?", startDate, endDate);
    }

    @Override
    public List<JournalDetail> findPostedJournalDetailsBetween(LocalDate startDate, LocalDate endDate) {
        return journalDetailRepository.findPostedJournalDetailsByAccountingDateBetween(startDate, endDate);
    }

    @Override
    public List<GlBalance> findGlBalances(
            LocalDate startDate, LocalDate endDate, String accountCode, String currencyCode) {
        return glBalanceRepository.findForQuery(startDate, endDate, accountCode, currencyCode);
    }

    @Override
    public List<SlBalance> findSlBalances(
            LocalDate startDate, LocalDate endDate, String accountCode,
            String businessPartnerCode, String departmentCode, String currencyCode) {
        return slBalanceRepository.findForQuery(
                startDate, endDate, accountCode, businessPartnerCode, departmentCode, currencyCode);
    }

    @Override
    public LedgerAggregateSummary calculateGlBalanceAggregate(
            LocalDate startDate, LocalDate endDate, String accountCode, String currencyCode, String amountBasis) {
        Object rawResult = glBalanceRepository.calculateGlBalanceAggregate(
                startDate, endDate, accountCode, currencyCode, amountBasis);
        if (rawResult == null) {
            return new LedgerAggregateSummary(0L, BigDecimal.ZERO);
        }
        Object[] row = (rawResult instanceof Object[]) ? (Object[]) rawResult : new Object[]{rawResult};
        long count = (row.length > 0 && row[0] != null) ? ((Number) row[0]).longValue() : 0L;
        BigDecimal sum = (row.length > 1 && row[1] != null) ? (BigDecimal) row[1] : BigDecimal.ZERO;
        return new LedgerAggregateSummary(count, sum);
    }

    private void bindGlBalance(PreparedStatement ps, GlBalance balance, LocalDateTime now) throws SQLException {
        ps.setString(1, balance.getAccountCode());
        ps.setString(2, balance.getCurrencyCode());
        ps.setObject(3, balance.getBalanceDate());
        ps.setString(4, periodValue(balance.getPeriod()));
        ps.setBigDecimal(5, zeroIfNull(balance.getBeginningBalance()));
        ps.setBigDecimal(6, zeroIfNull(balance.getDebitAmount()));
        ps.setBigDecimal(7, zeroIfNull(balance.getCreditAmount()));
        ps.setBigDecimal(8, zeroIfNull(balance.getEndingBalance()));
        ps.setObject(9, balance.getCreatedAt() == null ? now : balance.getCreatedAt());
        ps.setObject(10, now);
    }

    private void bindSlBalanceInsert(PreparedStatement ps, SlBalance balance, LocalDateTime now) throws SQLException {
        ps.setString(1, balance.getAccountCode());
        ps.setString(2, balance.getBusinessPartnerCode());
        ps.setString(3, balance.getDepartmentCode());
        ps.setString(4, balance.getCurrencyCode());
        ps.setObject(5, balance.getBalanceDate());
        ps.setString(6, periodValue(balance.getPeriod()));
        ps.setBigDecimal(7, zeroIfNull(balance.getBeginningBalance()));
        ps.setBigDecimal(8, zeroIfNull(balance.getDebitAmount()));
        ps.setBigDecimal(9, zeroIfNull(balance.getCreditAmount()));
        ps.setBigDecimal(10, zeroIfNull(balance.getEndingBalance()));
        ps.setObject(11, balance.getCreatedAt() == null ? now : balance.getCreatedAt());
        ps.setObject(12, now);
    }

    private void bindSlBalanceUpdate(PreparedStatement ps, SlBalance balance, LocalDateTime now) throws SQLException {
        ps.setBigDecimal(1, zeroIfNull(balance.getBeginningBalance()));
        ps.setBigDecimal(2, zeroIfNull(balance.getDebitAmount()));
        ps.setBigDecimal(3, zeroIfNull(balance.getCreditAmount()));
        ps.setBigDecimal(4, zeroIfNull(balance.getEndingBalance()));
        ps.setObject(5, now);
        ps.setString(6, balance.getAccountCode());
        ps.setString(7, balance.getBusinessPartnerCode());
        ps.setString(8, balance.getBusinessPartnerCode());
        ps.setString(9, balance.getDepartmentCode());
        ps.setString(10, balance.getDepartmentCode());
        ps.setString(11, balance.getCurrencyCode());
        ps.setObject(12, balance.getBalanceDate());
        ps.setString(13, periodValue(balance.getPeriod()));
    }

    private String glUpsertSql() {
        return switch (dialect()) {
            case H2 -> "MERGE INTO gl_balances (" + GL_COLUMNS + ") "
                    + "KEY(account_code, currency_code, balance_date, period) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            case POSTGRESQL -> "INSERT INTO gl_balances (" + GL_COLUMNS + ") "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (account_code, currency_code, balance_date, period) DO UPDATE SET "
                    + "beginning_balance = EXCLUDED.beginning_balance, "
                    + "debit_amount = EXCLUDED.debit_amount, "
                    + "credit_amount = EXCLUDED.credit_amount, "
                    + "ending_balance = EXCLUDED.ending_balance, "
                    + "updated_at = EXCLUDED.updated_at";
            case MYSQL -> "INSERT INTO gl_balances (" + GL_COLUMNS + ") "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "beginning_balance = VALUES(beginning_balance), "
                    + "debit_amount = VALUES(debit_amount), "
                    + "credit_amount = VALUES(credit_amount), "
                    + "ending_balance = VALUES(ending_balance), "
                    + "updated_at = VALUES(updated_at)";
        };
    }

    private DatabaseDialect dialect() {
        return jdbcTemplate.execute((ConnectionCallback<DatabaseDialect>) connection ->
                DatabaseDialect.from(connection.getMetaData().getDatabaseProductName()));
    }

    private String periodValue(YearMonth period) {
        return period == null ? null : period.toString();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private enum DatabaseDialect {
        H2,
        POSTGRESQL,
        MYSQL;

        static DatabaseDialect from(String productName) {
            String normalized = productName == null ? "" : productName.toLowerCase();
            if (normalized.contains("h2")) {
                return H2;
            }
            if (normalized.contains("postgres")) {
                return POSTGRESQL;
            }
            if (normalized.contains("mysql") || normalized.contains("mariadb")) {
                return MYSQL;
            }
            throw new IllegalStateException("Unsupported ledger JDBC bulk upsert database: " + productName);
        }
    }
}
