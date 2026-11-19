package com.ho.account.deposit.application.service;

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
@ContextConfiguration(classes = DepositJournalCreationIntegrationTest.JournalConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DepositJournalCreationIntegrationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan({"com.ho.account.journalledger.domain", "com.ho.account.deposit.domain"})
    @EnableJpaRepositories({"com.ho.account.journalledger.infrastructure.persistence.repository", "com.ho.account.deposit.infrastructure.adapter.out.persistence"})
    @Import({JournalPersistenceAdapter.class, JournalCreationOperationPersistenceAdapter.class,
            com.ho.account.deposit.infrastructure.adapter.out.persistence.DepositAccountPersistenceAdapter.class,
            com.ho.account.deposit.infrastructure.adapter.out.persistence.DepositTransactionalOutboxAdapter.class})
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

    JournalEntryCommand changedAmount(JournalEntryCommand original) {
        List<JournalLineCommand> lines = original.lines().stream().map(line -> new JournalLineCommand(
                line.drcrType(), line.accountCode(), line.amount().add(BigDecimal.ONE),
                line.baseAmount().add(BigDecimal.ONE), line.departmentCode(),
                line.businessPartnerCode(), line.detailDescription())).toList();
        return new JournalEntryCommand(original.slipDate(), original.accountingDate(), original.description(),
                original.entryType(), original.currencyCode(), original.exchangeRate(), original.createdBy(),
                original.auditUser(), original.lineageSourceType(), original.lineageSourceId(), original.slipNo(),
                original.operationKey(), original.sourceRequestFingerprint(), lines);
    }

    @Autowired com.ho.account.deposit.application.port.out.DepositAccountPersistencePort accounts;
    @Autowired com.ho.account.contracts.outbox.OutboxPort outbox;
    @Autowired com.ho.account.deposit.infrastructure.adapter.out.persistence.SpringDataDepositOutboxRepository outboxRows;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

    @Test
    void legacyPersistedPayloadUsesItsOriginalOutboxKeyAndExplicitKeysRemainIntact() {
        LocalDate date = LocalDate.of(2026, 5, 11);
        var legacy = new JournalEntryCommand(date, date, "Legacy initial deposit", "DEPOSIT_INITIAL_DEPOSIT",
                "KRW", null, "SYSTEM", "SYSTEM", "DEPOSIT_ACCOUNT", "DEP-LEGACY",
                List.of(new JournalLineCommand("DEBIT", "10100", BigDecimal.TEN, BigDecimal.TEN, null, null, "Cash"),
                        new JournalLineCommand("CREDIT", "20200", BigDecimal.TEN, BigDecimal.TEN, null, null, "Deposit")));
        var event = com.ho.account.contracts.outbox.JournalOutboxEvent.createPending(
                "DEPOSIT", "DEPOSIT_ACCOUNT", "DEP-LEGACY", legacy, "DEPOSIT_ACCOUNT:DEP-LEGACY");
        outbox.saveJournalEvent(event);
        assertThat(outboxRows.findByIdempotencyKey(event.getIdempotencyKey()).orElseThrow().getPayload())
                .contains("\"operationKey\":null");
        JournalEntryCommand restored = outbox.findJournalEventById(event.getEventId()).orElseThrow().getCommand();
        assertThat(restored.operationKey()).isEqualTo(event.getIdempotencyKey());
        var first = provider.createDraftEntry(restored);
        assertThat(provider.createDraftEntry(outbox.findJournalEventById(event.getEventId()).orElseThrow().getCommand())
                .journalEntryId()).isEqualTo(first.journalEntryId());
        assertThatThrownBy(() -> provider.createDraftEntry(changedAmount(restored)))
                .isInstanceOf(JournalCreationConflictException.class);
        assertJournalCount(1);

        var explicit = new JournalEntryCommand(date, date, legacy.description(), legacy.entryType(), "KRW", null,
                "SYSTEM", "SYSTEM", "DEPOSIT_ACCOUNT", "DEP-LEGACY", null,
                "DEPOSIT:CORRECTION:v2", legacy.lines());
        var explicitEvent = com.ho.account.contracts.outbox.JournalOutboxEvent.createPending(
                "DEPOSIT", "DEPOSIT_ACCOUNT", "DEP-LEGACY", explicit, "TRANSPORT-EVENT-2");
        outbox.saveJournalEvent(explicitEvent);
        JournalEntryCommand restoredExplicit = outbox.findJournalEventById(explicitEvent.getEventId()).orElseThrow().getCommand();
        assertThat(restoredExplicit.operationKey()).isEqualTo("DEPOSIT:CORRECTION:v2");
        provider.createDraftEntry(restoredExplicit);
        assertJournalCount(2);
    }

    @Test
    void committedInitialOutboxSurvivesLostResponseAndConflictingReplay() {
        var mapping = mock(com.ho.account.deposit.application.port.out.DepositAccountMappingPort.class);
        when(mapping.resolveInitialDepositAccounts(any())).thenReturn(
                new com.ho.account.deposit.application.port.out.DepositAccountMappingPort.InitialDepositAccounts("10100", "20200"));
        MasterDataQueryPort master = mock(MasterDataQueryPort.class);
        when(master.findAccountSubject(any())).thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        JournalPostingPort lostResponse = mock(JournalPostingPort.class);
        // Model the remote Journal commit independently of Deposit's source transaction.
        var journalTransaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        journalTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        when(lostResponse.createDraftEntry(any())).thenAnswer(invocation -> {
            journalTransaction.execute(status -> provider.createDraftEntry(invocation.getArgument(0)));
            throw new IllegalStateException("simulated response loss after journal commit");
        });
        var service = new DepositService(accounts, mapping, master, lostResponse, outbox,
                new com.ho.account.contracts.outbox.JournalOutboxRelayService(outbox, lostResponse), transactions);
        String account = new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status ->
                service.openAccount(new com.ho.account.deposit.application.port.in.OpenAccountUseCase.OpenAccountCommand(
                        "CUST-1", "PROD-1", "KRW", new BigDecimal("100.00"), BigDecimal.ZERO)));
        var row = outboxRows.findByIdempotencyKey("DEPOSIT_ACCOUNT:" + account).orElseThrow();
        var event = outbox.findJournalEventById(row.getEventId()).orElseThrow();
        assertThat(event.getCommand().operationKey()).isEqualTo(row.getIdempotencyKey());
        assertJournalCount(1);
        var first = provider.createDraftEntry(event.getCommand());
        var second = provider.createDraftEntry(outbox.findJournalEventById(row.getEventId()).orElseThrow().getCommand());
        assertThat(second.journalEntryId()).isEqualTo(first.journalEntryId());
        new com.ho.account.contracts.outbox.JournalOutboxRelayService(outbox, provider).publish(event);
        assertThat(outboxRows.findByIdempotencyKey(row.getIdempotencyKey()).orElseThrow().getStatus())
                .isEqualTo(com.ho.account.contracts.outbox.OutboxStatus.PUBLISHED);
        assertThatThrownBy(() -> provider.createDraftEntry(changedAmount(event.getCommand())))
                .isInstanceOf(JournalCreationConflictException.class);
        assertJournalCount(1);

        // Two subsequent deposits on the same account are separate durable business events.
        var balanceService = new DepositService(accounts, mapping, master, provider, outbox,
                new com.ho.account.contracts.outbox.JournalOutboxRelayService(outbox, provider), transactions);
        balanceService.deposit(account, new BigDecimal("10.00"));
        balanceService.deposit(account, new BigDecimal("10.00"));
        var pending = outboxRows.findAll().stream()
                .filter(saved -> "DEPOSIT_TRANSACTION".equals(saved.getLineageSourceType())).toList();
        assertThat(pending).hasSize(2);
        for (var transaction : pending) {
            JournalEntryCommand command = outbox.findJournalEventById(transaction.getEventId()).orElseThrow().getCommand();
            assertThat(command.operationKey()).isEqualTo(transaction.getIdempotencyKey());
            var result = provider.createDraftEntry(command);
            assertThat(provider.createDraftEntry(command).journalEntryId()).isEqualTo(result.journalEntryId());
            assertThatThrownBy(() -> provider.createDraftEntry(changedAmount(command)))
                    .isInstanceOf(JournalCreationConflictException.class);
        }
        assertJournalCount(3);
    }

}
