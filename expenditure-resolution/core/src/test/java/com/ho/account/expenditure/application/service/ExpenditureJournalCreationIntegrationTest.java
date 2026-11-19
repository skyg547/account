package com.ho.account.expenditure.application.service;

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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.mapping-resources=META-INF/journal-ledger-orm.xml"})
@ContextConfiguration(classes = ExpenditureJournalCreationIntegrationTest.JournalConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExpenditureJournalCreationIntegrationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.domain")
    @EnableJpaRepositories("com.ho.account.journalledger.infrastructure.persistence.repository")
    @Import({JournalPersistenceAdapter.class, JournalCreationOperationPersistenceAdapter.class})
    static class JournalConfiguration {
        @Bean
        JournalEntryService journalService(JournalPersistencePort journals,
                JournalCreationOperationPersistencePort operations) {
            return new JournalEntryService(journals, operations, mock(JournalRuleEngine.class),
                    mock(PostingService.class), new JournalValidationEngine(List.of(
                            new BalanceValidationFilter(), new ClosingLockValidationFilter(date -> false))));
        }
        @Bean
        JournalPostingPort journalProvider(JournalEntryService service) {
            return new JournalPostingAdapter(service);
        }
    }

    @Autowired JournalPostingPort provider;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepareDurableReceiptStore() {
        jdbc.update("DELETE FROM journal_creation_operations");
        jdbc.update("DELETE FROM journal_details");
        jdbc.update("DELETE FROM journal_entries");
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS journal_slip_no_seq START WITH 1");
        jdbc.execute("CREATE TABLE IF NOT EXISTS journal_creation_operation_locks (lock_id INTEGER PRIMARY KEY)");
        for (int stripe = 0; stripe < 256; stripe++) {
            jdbc.update("MERGE INTO journal_creation_operation_locks (lock_id) KEY(lock_id) VALUES (?)", stripe);
        }
    }

    void assertJournalCount(int count) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isEqualTo(count);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isEqualTo(count);
    }


    @Test
    void approvalRetryUsesStoredResolutionIdentityAndChangedFinancialContentConflicts() {
        var resolutions = mock(com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort.class);
        MasterDataQueryPort master = mock(MasterDataQueryPort.class);
        when(master.findDepartment(any())).thenReturn(Optional.of(new DepartmentRef("D001", "Finance", "COST_CENTER")));
        when(master.findAccountSubject(any())).thenAnswer(invocation ->
                Optional.of(new AccountSubjectRef(invocation.getArgument(0), "Account", false, false)));
        when(master.findBusinessPartner(any())).thenReturn(Optional.of(new BusinessPartnerRef("BP001", "Vendor", "VENDOR", true)));
        var first = resolution("REQ-20260511-001", "100.00");
        var retry = resolution("REQ-20260511-001", "100.00");
        var changed = resolution("REQ-20260511-001", "101.00");
        var independent = resolution("REQ-20260511-002", "100.00");
        when(resolutions.findById(1L)).thenReturn(Optional.of(first), Optional.of(retry), Optional.of(changed));
        when(resolutions.findById(2L)).thenReturn(Optional.of(independent));
        List<JournalEntryCommand> commands = new ArrayList<>();
        JournalPostingPort losingResponse = mock(JournalPostingPort.class);
        when(losingResponse.createDraftEntry(any())).thenAnswer(invocation -> {
            JournalEntryCommand command = invocation.getArgument(0);
            commands.add(command);
            JournalPostingResult result = provider.createDraftEntry(command);
            if (commands.size() == 1) throw new IllegalStateException("simulated response loss after journal commit");
            return result;
        });
        var service = new ExpenditureResolutionService(resolutions, losingResponse, master,
                mock(BudgetService.class), mock(com.ho.account.contracts.asset.AssetRegistrationPort.class),
                mock(com.ho.account.contracts.tax.TaxInvoiceQueryPort.class));
        assertThatThrownBy(() -> service.approveResolution(1L)).isInstanceOf(IllegalStateException.class);
        assertJournalCount(1);
        service.approveResolution(1L);
        assertThat(retry.getJournalEntryId()).isNotNull();
        assertThat(commands.get(1)).isEqualTo(commands.get(0));
        assertThat(commands.get(0).operationKey()).isEqualTo("EXPENDITURE_RESOLUTION:APPROVAL:v1");
        assertThatThrownBy(() -> service.approveResolution(1L)).isInstanceOf(JournalCreationConflictException.class);
        assertJournalCount(1);
        service.approveResolution(2L);
        assertThat(independent.getJournalEntryId()).isNotEqualTo(retry.getJournalEntryId());
        assertJournalCount(2);
    }

    private com.ho.account.expenditure.domain.ExpenditureResolution resolution(String number, String amount) {
        var resolution = com.ho.account.expenditure.domain.ExpenditureResolution.create(number, "Approved expense",
                LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 12), "D001", "PAY001", "maker");
        resolution.addDetail(com.ho.account.expenditure.domain.ExpenditureDetail.create(
                "EXP001", new BigDecimal(amount), "BP001", "Expense"));
        resolution.requestApproval();
        return resolution;
    }

}
