package com.ho.account.loan.support;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.journalledger.application.port.out.*;
import com.ho.account.journalledger.application.service.journal.*;
import com.ho.account.journalledger.application.service.journal.validator.*;
import com.ho.account.journalledger.application.service.ledger.*;
import com.ho.account.journalledger.infrastructure.persistence.*;
import com.ho.account.loan.application.port.out.*;
import com.ho.account.loan.infrastructure.adapter.LoanJournalAdapter;
import com.ho.account.loan.infrastructure.persistence.LoanPersistenceAdapter;
import com.ho.account.loan.service.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.mockito.Mockito.mock;

/** Real Journal receipts and Loan persistence; only reference data and ledger sinks are controlled ports. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan({"com.ho.account.loan.domain", "com.ho.account.journalledger.domain"})
@EnableJpaRepositories({"com.ho.account.loan.infrastructure.persistence",
        "com.ho.account.journalledger.infrastructure.persistence.repository"})
@Import({JournalPersistenceAdapter.class, JournalCreationOperationPersistenceAdapter.class,
        LoanPersistenceAdapter.class, LoanJournalAdapter.class, LoanService.class})
public class DurableLoanJournalTestConfiguration {
    @Bean DataSource dataSource() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:loan-receipt-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var scripts = new PathMatchingResourcePatternResolver().getResources("classpath:db/journal-migration/V*.sql");
        Arrays.sort(scripts, Comparator.comparingInt(resource -> Integer.parseInt(
                resource.getFilename().substring(1, resource.getFilename().indexOf("__")))));
        if (scripts.length < 12) throw new IllegalStateException("Production Journal migrations are required");
        new ResourceDatabasePopulator(scripts).execute(dataSource);
        new ResourceDatabasePopulator(
                new ClassPathResource("db/postgresql-migration/V30__init_loan_schema.sql"),
                new ClassPathResource("db/migration/V31__loan_owned_journal_references.sql"),
                new ClassPathResource("db/migration/V32__loan_accrual_journal_reference.sql"),
                new ClassPathResource("db/migration/V33__loan_consistency_and_reference_boundaries.sql"),
                new ClassPathResource("db/migration/V34__loan_source_operation_identity.sql")).execute(dataSource);
        return dataSource;
    }
    @Bean LoanAccountingProperties properties() {
        var p = new LoanAccountingProperties();
        p.setCashAccountCode("101900"); p.setLoanReceivableAccountCode("131900");
        p.setDeferredAssetAccountCode("171900"); p.setRecognizedIncomeAccountCode("401900");
        p.setJournalApproverActor("service:loan-checker"); return p;
    }
    @Bean LoanReferenceDataPort references() { return mock(LoanReferenceDataPort.class); }
    @Bean EIRCalculator calculator() { return mock(EIRCalculator.class); }
    @Bean LedgerEntryPersistencePort ledgerEntries() { return mock(LedgerEntryPersistencePort.class); }
    @Bean LedgerService ledgerService() { return mock(LedgerService.class); }
    @Bean AccountingPeriodStatusPort periods() { return mock(AccountingPeriodStatusPort.class); }
    @Bean JournalEntryService journals(JournalPersistencePort journals,
            JournalCreationOperationPersistencePort operations, LedgerEntryPersistencePort entries,
            LedgerService ledger, AccountingPeriodStatusPort periods) {
        return new JournalEntryService(journals, operations, mock(JournalRuleEngine.class),
                new PostingService(journals, entries, ledger, new ClosingLockValidationFilter(periods)),
                new JournalValidationEngine(List.of(new BalanceValidationFilter())));
    }
}
