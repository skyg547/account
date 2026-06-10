package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JdbcLedgerBulkPersistenceAdapterTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:ledger_bulk_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        jdbcTemplate = new JdbcTemplate(dataSource);
        createTables();
    }

    @Test
    void insertsGlAndSlEntriesWithJdbcBatch() {
        JdbcLedgerEntryBulkPersistenceAdapter adapter = new JdbcLedgerEntryBulkPersistenceAdapter(jdbcTemplate);
        JournalDetail detail = new JournalDetail();
        detail.setId(101L);

        adapter.saveGlEntries(List.of(glEntry(detail)));
        adapter.saveSlEntries(List.of(slEntry(detail)));

        Integer glCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gl_entries", Integer.class);
        Integer slCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sl_entries", Integer.class);
        assertThat(glCount).isEqualTo(1);
        assertThat(slCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT base_dr_amount FROM gl_entries", BigDecimal.class))
                .isEqualByComparingTo("100.00");
        assertThat(jdbcTemplate.queryForObject("SELECT business_partner_code FROM sl_entries", String.class))
                .isEqualTo("BP-001");
    }

    @Test
    void upsertsGlAndSlBalancesWithJdbcBatch() {
        JdbcLedgerBalanceBulkPersistenceAdapter adapter = new JdbcLedgerBalanceBulkPersistenceAdapter(
                jdbcTemplate,
                mock(GlBalanceRepository.class),
                mock(SlBalanceRepository.class),
                mock(JournalDetailRepository.class)
        );

        adapter.saveGlBalances(List.of(glBalance("100.00", "0.00")));
        adapter.saveGlBalances(List.of(glBalance("125.00", "25.00")));

        Integer glCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gl_balances", Integer.class);
        assertThat(glCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT debit_amount FROM gl_balances", BigDecimal.class))
                .isEqualByComparingTo("125.00");
        assertThat(jdbcTemplate.queryForObject("SELECT credit_amount FROM gl_balances", BigDecimal.class))
                .isEqualByComparingTo("25.00");

        adapter.saveSlBalances(List.of(slBalance("50.00", "0.00")));
        adapter.saveSlBalances(List.of(slBalance("75.00", "5.00")));

        Integer slCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sl_balances", Integer.class);
        assertThat(slCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT debit_amount FROM sl_balances", BigDecimal.class))
                .isEqualByComparingTo("75.00");
        assertThat(jdbcTemplate.queryForObject("SELECT credit_amount FROM sl_balances", BigDecimal.class))
                .isEqualByComparingTo("5.00");
    }

    private GlEntry glEntry(JournalDetail detail) {
        GlEntry entry = new GlEntry();
        entry.setJournalDetail(detail);
        entry.setAccountCode("10100");
        entry.setCurrencyCode("KRW");
        entry.setFiscalYear("2026");
        entry.setFiscalPeriod("06");
        entry.setPostingDate(LocalDate.of(2026, 6, 10));
        entry.setDrAmount(new BigDecimal("100.00"));
        entry.setCrAmount(BigDecimal.ZERO);
        entry.setBaseDrAmount(new BigDecimal("100.00"));
        entry.setBaseCrAmount(BigDecimal.ZERO);
        entry.setSummary("cash debit");
        entry.setLineageSourceType("TEST");
        entry.setLineageSourceId("SRC-1");
        return entry;
    }

    private SlEntry slEntry(JournalDetail detail) {
        SlEntry entry = new SlEntry();
        entry.setJournalDetail(detail);
        entry.setAccountCode("10100");
        entry.setBusinessPartnerCode("BP-001");
        entry.setDepartmentCode("D-10");
        entry.setCurrencyCode("KRW");
        entry.setFiscalYear("2026");
        entry.setFiscalPeriod("06");
        entry.setPostingDate(LocalDate.of(2026, 6, 10));
        entry.setDrAmount(new BigDecimal("100.00"));
        entry.setCrAmount(BigDecimal.ZERO);
        entry.setBaseDrAmount(new BigDecimal("100.00"));
        entry.setBaseCrAmount(BigDecimal.ZERO);
        entry.setSummary("cash debit by partner");
        entry.setLineageSourceType("TEST");
        entry.setLineageSourceId("SRC-1");
        return entry;
    }

    private GlBalance glBalance(String debit, String credit) {
        GlBalance balance = new GlBalance();
        balance.setAccountCode("10100");
        balance.setCurrencyCode("KRW");
        balance.setBalanceDate(LocalDate.of(2026, 6, 10));
        balance.setPeriod(YearMonth.of(2026, 6));
        balance.setBeginningBalance(BigDecimal.ZERO);
        balance.setDebitAmount(new BigDecimal(debit));
        balance.setCreditAmount(new BigDecimal(credit));
        balance.recalculate();
        return balance;
    }

    private SlBalance slBalance(String debit, String credit) {
        SlBalance balance = new SlBalance();
        balance.setAccountCode("10100");
        balance.setBusinessPartnerCode(null);
        balance.setDepartmentCode(null);
        balance.setCurrencyCode("KRW");
        balance.setBalanceDate(LocalDate.of(2026, 6, 10));
        balance.setPeriod(YearMonth.of(2026, 6));
        balance.setBeginningBalance(BigDecimal.ZERO);
        balance.setDebitAmount(new BigDecimal(debit));
        balance.setCreditAmount(new BigDecimal(credit));
        balance.recalculate();
        return balance;
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE gl_entries (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    journal_detail_id BIGINT,
                    account_code VARCHAR(50) NOT NULL,
                    currency_code VARCHAR(3),
                    fiscal_year VARCHAR(4),
                    fiscal_period VARCHAR(2),
                    posting_date DATE,
                    dr_amount DECIMAL(19, 2),
                    cr_amount DECIMAL(19, 2),
                    base_dr_amount DECIMAL(19, 2),
                    base_cr_amount DECIMAL(19, 2),
                    summary VARCHAR(255),
                    lineage_source_type VARCHAR(100),
                    lineage_source_id VARCHAR(100)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sl_entries (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    journal_detail_id BIGINT,
                    account_code VARCHAR(50) NOT NULL,
                    business_partner_code VARCHAR(50),
                    dept_code VARCHAR(50),
                    currency_code VARCHAR(3),
                    fiscal_year VARCHAR(4),
                    fiscal_period VARCHAR(2),
                    posting_date DATE,
                    dr_amount DECIMAL(19, 2),
                    cr_amount DECIMAL(19, 2),
                    base_dr_amount DECIMAL(19, 2),
                    base_cr_amount DECIMAL(19, 2),
                    summary VARCHAR(255),
                    lineage_source_type VARCHAR(100),
                    lineage_source_id VARCHAR(100)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE gl_balances (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    account_code VARCHAR(50) NOT NULL,
                    currency_code VARCHAR(3) NOT NULL,
                    balance_date DATE NOT NULL,
                    period VARCHAR(7) NOT NULL,
                    beginning_balance DECIMAL(19, 2) NOT NULL,
                    debit_amount DECIMAL(19, 2) NOT NULL,
                    credit_amount DECIMAL(19, 2) NOT NULL,
                    ending_balance DECIMAL(19, 2) NOT NULL,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    CONSTRAINT uk_gl_balance_key UNIQUE(account_code, currency_code, balance_date, period)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sl_balances (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    account_code VARCHAR(50) NOT NULL,
                    bp_code VARCHAR(50),
                    dept_code VARCHAR(50),
                    currency_code VARCHAR(3) NOT NULL,
                    balance_date DATE NOT NULL,
                    period VARCHAR(7) NOT NULL,
                    beginning_balance DECIMAL(19, 2) NOT NULL,
                    debit_amount DECIMAL(19, 2) NOT NULL,
                    credit_amount DECIMAL(19, 2) NOT NULL,
                    ending_balance DECIMAL(19, 2) NOT NULL,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
    }
}
