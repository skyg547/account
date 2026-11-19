package com.ho.account.closing.infrastructure.external;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ho.account.closing.application.port.out.*;
import com.ho.account.closing.application.service.AnnualClosingService;
import com.ho.account.closing.domain.ApprovedRetainedEarningsMapping;
import com.ho.account.contracts.journal.*;
import com.ho.account.contracts.masterdata.*;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalCreationOperationPersistencePort;
import com.ho.account.journalledger.application.service.journal.*;
import com.ho.account.journalledger.application.service.journal.validator.*;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.infrastructure.adapter.JournalPostingAdapter;
import com.ho.account.journalledger.infrastructure.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Actual Closing producers and durable Journal receipt, with a synthetic annual source snapshot. */
@SpringBootTest(classes = ClosingJournalCreationIdentityIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=closing-creation-test", "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.mapping-resources=META-INF/journal-ledger-orm.xml", "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=false", "spring.sql.init.mode=never", "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false", "spring.cloud.vault.enabled=false", "eureka.client.enabled=false"})
class ClosingJournalCreationIdentityIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 12, 31);
    @Autowired JournalEntryService journals;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void clear() {
        jdbc.update("DELETE FROM journal_creation_operations");
        jdbc.update("DELETE FROM journal_details"); jdbc.update("DELETE FROM journal_entries");
    }

    @ParameterizedTest @ValueSource(strings = {"FX_VALUATION", "ECL_PROVISION"})
    void adjustmentFreshRetryAndChangedContentUseDurableOperation(String type) {
        // Force the create path on every call, as after a lost acknowledgement/provider restart.
        JournalQueryPort query = mock(JournalQueryPort.class);
        var firstAdapter = new JournalLedgerClosingJournalEntryAdapter(new JournalPostingAdapter(journals), query);
        var first = firstAdapter.createDraftAdjustment(command(type, "100"));
        var restarted = new JournalLedgerClosingJournalEntryAdapter(new JournalPostingAdapter(journals), query);
        assertThat(restarted.createDraftAdjustment(command(type, "100.00")).journalEntryId())
                .isEqualTo(first.journalEntryId());
        assertThatThrownBy(() -> restarted.createDraftAdjustment(command(type, "101")))
                .isInstanceOf(JournalCreationConflictException.class);
        assertThat(count()).isOne();
        assertThat(jdbc.queryForObject("SELECT operation_key FROM journal_creation_operations", String.class))
                .isEqualTo(type + "_V1");
    }

    @Test void annualActualProducerUsesSnapshotIdentityWithExplicitTransferOperation() {
        var source = new JournalSummary();
        source.setId(1L); source.setSlipNo("SOURCE-1"); source.setSlipDate(DATE);
        source.setAccountingDate(DATE); source.setDescription("source"); source.setStatus("POSTED");
        source.setEntryType("NORMAL"); source.setCurrencyCode("KRW");
        source.setLineageSourceType("SOURCE"); source.setLineageSourceId("1");
        var credit = new JournalDetailSummary();
        credit.setId(1L); credit.setSlipNo("SOURCE-1"); credit.setAccountingDate(DATE);
        credit.setHeaderDescription(source.getDescription());
        credit.setAccountCode("41000"); credit.setAccountCategory("REVENUE");
        credit.setSide(JournalSide.CREDIT); credit.setAmount(new BigDecimal("100")); credit.setBaseAmount(new BigDecimal("100"));
        AnnualJournalReadPort read = (start, end, first, digest) -> {
            first.accept(source, List.of(credit)); digest.accept(source, List.of(credit));
            return new AnnualJournalReadPort.SourceControl(1, 1, BigDecimal.ZERO, new BigDecimal("100"), 1);
        };
        MasterDataQueryPort master = mock(MasterDataQueryPort.class);
        when(master.findAccountSubjectAt(anyString(), any())).thenAnswer(invocation -> Optional.of(
                new AccountSubjectRef(invocation.getArgument(0), "synthetic", false, false, "CREDIT",
                        "35000".equals(invocation.getArgument(0)) ? "EQUITY" : "REVENUE")));
        AtomicReference<JournalEntryCommand> captured = new AtomicReference<>();
        JournalPostingPort posting = new JournalPostingAdapter(journals) {
            @Override public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
                captured.set(command); return super.createDraftEntry(command);
            }
        };
        var annual = new AnnualClosingService(posting, master,
                year -> new ApprovedRetainedEarningsMapping("ENTITY-01", year, "35000", true,
                        "controller", "CHG-955"), read);
        annual.performIncomeStatementClosing(2026);
        var first = captured.get();
        annual.performIncomeStatementClosing(2026);
        assertThat(count()).isOne();
        assertThat(first.operationKey()).isEqualTo("ANNUAL_INCOME_TRANSFER_V1");
        assertThat(captured.get().lineageSourceId()).isEqualTo(first.lineageSourceId());
        var changedLines = first.lines().stream().map(line -> new JournalLineCommand(line.drcrType(),
                line.accountCode(), new BigDecimal("101"), new BigDecimal("101"), null, null,
                line.detailDescription())).toList();
        assertThatThrownBy(() -> posting.createDraftEntry(new JournalEntryCommand(first.slipDate(), first.accountingDate(),
                first.description(), first.entryType(), first.currencyCode(), first.exchangeRate(), first.createdBy(),
                first.auditUser(), first.lineageSourceType(), first.lineageSourceId(), first.slipNo(),
                first.operationKey(), changedLines))).isInstanceOf(JournalCreationConflictException.class);
        assertThat(count()).isOne();
    }

    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class); }
    private ClosingJournalEntryCommand command(String type, String value) {
        BigDecimal amount = new BigDecimal(value);
        return new ClosingJournalEntryCommand(DATE, DATE, "synthetic adjustment", "CLOSING_ADJUSTMENT",
                "SYSTEM", "SYSTEM", type, type.equals("FX_VALUATION") ? "955|11000|USD" : "ECLSNAP:955-group-a",
                "KRW", BigDecimal.ONE, type.equals("FX_VALUATION") ? "FX-955-1" : "ECL-955-1", List.of(
                        new ClosingJournalLineCommand(ClosingJournalSide.DEBIT, "11000", amount, amount, "adjustment"),
                        new ClosingJournalLineCommand(ClosingJournalSide.CREDIT, "41000", amount, amount, "adjustment")));
    }
    @Configuration(proxyBeanMethods = false) @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.domain")
    @EnableJpaRepositories("com.ho.account.journalledger.infrastructure.persistence.repository")
    @Import({JournalPersistenceAdapter.class, JournalCreationOperationPersistenceAdapter.class})
    static class TestApplication {
        @Bean DataSource dataSource() throws Exception {
            var ds = new DriverManagerDataSource("jdbc:h2:mem:closing-receipt-" + UUID.randomUUID()
                    + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
            var scripts = new PathMatchingResourcePatternResolver().getResources("classpath:db/journal-migration/V*.sql");
            Arrays.sort(scripts, Comparator.comparingInt(resource -> Integer.parseInt(
                    resource.getFilename().substring(1, resource.getFilename().indexOf("__")))));
            if (scripts.length < 12) throw new IllegalStateException("Production Journal migrations are required");
            new ResourceDatabasePopulator(scripts).execute(ds); return ds;
        }
        @Bean JournalEntryService journals(JournalPersistencePort journals, JournalCreationOperationPersistencePort operations) {
            return new JournalEntryService(journals, operations, mock(JournalRuleEngine.class), mock(PostingService.class),
                    new JournalValidationEngine(List.of(new BalanceValidationFilter())));
        }
    }
}
