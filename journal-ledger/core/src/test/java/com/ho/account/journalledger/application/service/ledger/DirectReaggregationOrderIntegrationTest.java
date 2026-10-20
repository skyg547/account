package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlBalanceRepository;
import com.ho.account.journalledger.infrastructure.persistence.JdbcBalanceReaggregationControlAdapter;
import com.ho.account.journalledger.infrastructure.persistence.JdbcLedgerBalanceBulkPersistenceAdapter;
import com.ho.account.journalledger.infrastructure.persistence.LedgerBalancePersistenceAdapter;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** The public core rebuild runs through real source queries and both real balance writers. */
@SpringBootTest(classes = DirectReaggregationOrderIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=direct-reaggregation-order-test",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true", "spring.flyway.locations=classpath:db/journal-migration",
        "spring.sql.init.mode=never", "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false", "spring.cloud.vault.enabled=false",
        "eureka.client.enabled=false"
})
class DirectReaggregationOrderIntegrationTest {
    private static final LocalDate FIRST = LocalDate.of(2026, 9, 1);
    private static final String DATABASE = "direct_reaggregation_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DATABASE
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
    }

    @Autowired @Qualifier("jpaRebuild") private LedgerService jpaRebuild;
    @Autowired @Qualifier("jdbcRebuild") private LedgerService jdbcRebuild;
    @Autowired private JournalEntryRepository journals;
    @Autowired private GlBalanceRepository glBalances;
    @Autowired private SlBalanceRepository slBalances;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactions;

    enum Mode { JPA, JDBC }

    @BeforeEach
    void reset() {
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            for (String table : List.of("gl_entries", "sl_entries", "gl_balances", "sl_balances",
                    "journal_details", "journal_entries")) {
                jdbc.update("DELETE FROM " + table);
            }
        });
    }

    static Stream<Arguments> cases() {
        return Stream.of(Mode.values()).flatMap(mode -> Stream.of(false, true)
                .map(reversed -> Arguments.of(mode, reversed)));
    }

    @ParameterizedTest(name = "{0}, source inserted in reverse order={1}")
    @MethodSource("cases")
    void directRebuildPreservesPriorOpeningAndActualDailyGlSlBalances(Mode mode, boolean reversed) {
        seedPriorPeriodOpening();
        List<Integer> insertionOrder = reversed ? List.of(1, 2, 0) : List.of(0, 1, 2);
        for (int day : insertionOrder) {
            seedPostedJournal(day, List.of("100.00", "20.00", "5.00").get(day));
        }

        service(mode).reaggregateLedgerBalancesForPeriod(FIRST, FIRST.plusDays(2));

        for (String table : List.of("gl_balances", "sl_balances")) {
            assertThat(amounts(table, "10100")).containsExactly(
                    new Amounts("50.00", "100.00", "0.00", "150.00"),
                    new Amounts("150.00", "20.00", "0.00", "170.00"),
                    new Amounts("170.00", "5.00", "0.00", "175.00"));
            assertThat(amounts(table, "40100")).containsExactly(
                    new Amounts("0.00", "0.00", "100.00", "-100.00"),
                    new Amounts("-100.00", "0.00", "20.00", "-120.00"),
                    new Amounts("-120.00", "0.00", "5.00", "-125.00"));
            assertThat(jdbc.queryForObject("SELECT ending_balance FROM " + table
                    + " WHERE account_code = '10100' AND balance_date = ?", BigDecimal.class, FIRST.minusDays(1)))
                    .isEqualByComparingTo("50.00");
        }
    }

    private LedgerService service(Mode mode) {
        return mode == Mode.JPA ? jpaRebuild : jdbcRebuild;
    }

    private List<Amounts> amounts(String table, String account) {
        return jdbc.query("SELECT beginning_balance, debit_amount, credit_amount, ending_balance"
                + " FROM " + table + " WHERE account_code = ? AND balance_date BETWEEN ? AND ?"
                + " ORDER BY balance_date", (rs, row) -> new Amounts(rs.getBigDecimal(1),
                rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)),
                account, FIRST, FIRST.plusDays(2));
    }

    private void seedPriorPeriodOpening() {
        transactions.executeWithoutResult(status -> {
            LocalDate date = FIRST.minusDays(1);
            GlBalance gl = new GlBalance();
            gl.setAccountCode("10100");
            gl.setCurrencyCode("KRW");
            gl.setBalanceDate(date);
            gl.setPeriod(YearMonth.from(date));
            gl.setDebitAmount(new BigDecimal("50.00"));
            gl.recalculate();
            glBalances.save(gl);
            SlBalance sl = new SlBalance();
            sl.setAccountCode("10100");
            sl.setBusinessPartnerCode("BP-001");
            sl.setDepartmentCode("D-10");
            sl.setCurrencyCode("KRW");
            sl.setBalanceDate(date);
            sl.setPeriod(YearMonth.from(date));
            sl.setDebitAmount(new BigDecimal("50.00"));
            sl.recalculate();
            slBalances.save(sl);
        });
    }

    private void seedPostedJournal(int day, String amount) {
        transactions.executeWithoutResult(status -> {
            LocalDate date = FIRST.plusDays(day);
            JournalEntry entry = new JournalEntry();
            entry.setSlipNo("ORDER-" + day);
            entry.setSlipDate(date);
            entry.setAccountingDate(date);
            entry.setCurrencyCode("KRW");
            entry.setLineageSourceType("TEST");
            entry.setLineageSourceId("ORDER-" + day);
            entry.setCreatedBy("maker");
            entry.addDetail(detail(JournalSide.DEBIT, "10100", amount));
            entry.addDetail(detail(JournalSide.CREDIT, "40100", amount));
            entry.initializeDraft();
            entry.requestApproval("maker");
            entry.approve("approver");
            journals.saveAndFlush(entry);
            entry.post("poster");
            journals.saveAndFlush(entry);
        });
    }

    private JournalDetail detail(JournalSide side, String account, String amount) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(account);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(amount));
        detail.setBusinessPartnerCode("BP-001");
        detail.setDepartmentCode("D-10");
        return detail;
    }

    private record Amounts(BigDecimal beginning, BigDecimal debit, BigDecimal credit, BigDecimal ending) {
        private Amounts(String beginning, String debit, String credit, String ending) {
            this(new BigDecimal(beginning), new BigDecimal(debit),
                    new BigDecimal(credit), new BigDecimal(ending));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.domain")
    @EnableJpaRepositories("com.ho.account.journalledger.domain")
    static class TestApplication {
        @Bean("jpaRebuild")
        LedgerService jpaRebuild(GlBalanceRepository gl, SlBalanceRepository sl,
                                 JournalDetailRepository details, EntityManager em, JdbcTemplate jdbc) {
            LedgerBalancePersistenceAdapter adapter = new LedgerBalancePersistenceAdapter(gl, sl, details);
            ReflectionTestUtils.setField(adapter, "entityManager", em);
            return new LedgerService(adapter, new JdbcBalanceReaggregationControlAdapter(jdbc));
        }

        @Bean("jdbcRebuild")
        LedgerService jdbcRebuild(GlBalanceRepository gl, SlBalanceRepository sl,
                                  JournalDetailRepository details, EntityManager em, JdbcTemplate jdbc) {
            LedgerBalancePersistencePort adapter = new JdbcLedgerBalanceBulkPersistenceAdapter(jdbc, gl, sl, details);
            ReflectionTestUtils.setField(adapter, "entityManager", em);
            return new LedgerService(adapter, new JdbcBalanceReaggregationControlAdapter(jdbc));
        }
    }
}
