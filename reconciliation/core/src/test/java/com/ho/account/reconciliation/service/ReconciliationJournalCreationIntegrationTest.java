package com.ho.account.reconciliation.service;

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
@ContextConfiguration(classes = ReconciliationJournalCreationIntegrationTest.JournalConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationJournalCreationIntegrationTest {
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
    void regeneratedRunIdsRetryOnePeriodWhileChangedAmountOrAccountsConflict() {
        var units = mock(com.ho.account.reconciliation.repository.ReconciliationUnitRepository.class);
        var rules = mock(com.ho.account.reconciliation.repository.ReconciliationRuleRepository.class);
        var reasons = mock(com.ho.account.reconciliation.repository.DifferenceReasonCodeRepository.class);
        var runs = mock(com.ho.account.reconciliation.repository.ReconciliationRunRepository.class);
        var differences = mock(com.ho.account.reconciliation.repository.ReconciliationDifferenceRepository.class);
        var snapshots = mock(com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort.class);
        JournalQueryPort query = mock(JournalQueryPort.class);
        LocalDate date = LocalDate.of(2026, 5, 11);
        var unit = new com.ho.account.reconciliation.domain.ReconciliationUnit();
        unit.setId(10L);
        unit.setName("Daily reconciliation");
        unit.setCriteriaJson("{\"adjustmentDebitAccountCode\":\"131000\",\"adjustmentCreditAccountCode\":\"211000\"}");
        when(units.findById(anyLong())).thenReturn(Optional.of(unit));
        var reason = new com.ho.account.reconciliation.domain.DifferenceReasonCode();
        reason.setCode("GENERIC_MISMATCH");
        reason.setName("Mismatch");
        reason.setAdjustable(true);
        when(reasons.findByCode("GENERIC_MISMATCH")).thenReturn(Optional.of(reason));
        java.util.concurrent.atomic.AtomicLong ids = new java.util.concurrent.atomic.AtomicLong(100);
        List<Long> createdRunIds = new ArrayList<>();
        when(runs.save(any())).thenAnswer(invocation -> {
            com.ho.account.reconciliation.domain.ReconciliationRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(ids.incrementAndGet());
                createdRunIds.add(run.getId());
            }
            return run;
        });
        when(differences.save(any())).thenAnswer(invocation -> {
            com.ho.account.reconciliation.domain.ReconciliationDifference diff = invocation.getArgument(0);
            if (diff.getId() == null) diff.setId(ids.incrementAndGet());
            return diff;
        });
        java.util.concurrent.atomic.AtomicReference<BigDecimal> amount =
                new java.util.concurrent.atomic.AtomicReference<>(new BigDecimal("50.00"));
        when(snapshots.loadSnapshot(any())).thenAnswer(invocation ->
                new com.ho.account.reconciliation.application.port.out.ExternalReconSnapshot(1, amount.get()));
        when(snapshots.loadItems(any())).thenAnswer(invocation -> List.of(
                com.ho.account.reconciliation.domain.ReconciliationItem.ofSource(
                        "SOURCE-1", date, "SOURCE-1", "", "", amount.get(), "Missing target")));
        List<JournalEntryCommand> commands = new ArrayList<>();
        JournalPostingPort losingResponse = mock(JournalPostingPort.class);
        when(losingResponse.createDraftEntry(any())).thenAnswer(invocation -> {
            JournalEntryCommand command = invocation.getArgument(0);
            commands.add(command);
            JournalPostingResult result = provider.createDraftEntry(command);
            if (commands.size() == 1) throw new IllegalStateException("simulated response loss after journal commit");
            return result;
        });
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var service = new ReconciliationService(units, rules, reasons, runs, differences, query, losingResponse,
                mapper, new com.ho.account.reconciliation.domain.ReconciliationAdjustmentPolicy(mapper), snapshots);
        var request = new com.ho.account.reconciliation.application.port.in.RunReconciliationCommand(10L, date, "operator");
        assertThatThrownBy(() -> service.performReconciliation(request)).isInstanceOf(RuntimeException.class);
        assertJournalCount(1);
        service.performReconciliation(request);
        assertThat(createdRunIds).hasSize(2).doesNotHaveDuplicates();
        assertThat(commands.get(1)).isEqualTo(commands.get(0));
        assertThat(commands.get(0).lineageSourceId()).isEqualTo("UNIT-10-DATE-2026-05-11");
        assertThat(commands.get(0).operationKey()).isEqualTo("RECONCILIATION:ADJUSTMENT:v1");
        amount.set(new BigDecimal("51.00"));
        assertThatThrownBy(() -> service.performReconciliation(request))
                .hasRootCauseInstanceOf(JournalCreationConflictException.class);
        amount.set(new BigDecimal("50.00"));
        unit.setCriteriaJson("{\"adjustmentDebitAccountCode\":\"131001\",\"adjustmentCreditAccountCode\":\"211000\"}");
        assertThatThrownBy(() -> service.performReconciliation(request))
                .hasRootCauseInstanceOf(JournalCreationConflictException.class);
        assertJournalCount(1);
        unit.setId(11L);
        service.performReconciliation(new com.ho.account.reconciliation.application.port.in.RunReconciliationCommand(11L, date, "operator"));
        assertJournalCount(2);
    }

}
