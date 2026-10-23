package com.ho.account.deposit.application.service;

import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.outbox.JournalOutboxEvent;
import com.ho.account.contracts.outbox.JournalOutboxRelayService;
import com.ho.account.contracts.outbox.OutboxPort;
import com.ho.account.contracts.outbox.OutboxStatus;
import com.ho.account.deposit.application.port.out.DepositAccountMappingPort;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositOutboxEntity;
import com.ho.account.deposit.domain.DepositStatus;
import com.ho.account.deposit.infrastructure.adapter.out.persistence.DepositAccountPersistenceAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.persistence.DepositTransactionalOutboxAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.persistence.SpringDataDepositAccountRepository;
import com.ho.account.deposit.infrastructure.adapter.out.persistence.SpringDataDepositOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.AdditionalAnswers.delegatesTo;

/** Verifies the actual JPA commit boundary used by deposit and withdrawal. */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes = DepositServiceTransactionIntegrationTest.TestApplication.class)
@Import({DepositAccountPersistenceAdapter.class, DepositTransactionalOutboxAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DepositServiceTransactionIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {DepositAccount.class, DepositOutboxEntity.class})
    @EnableJpaRepositories(basePackageClasses = {SpringDataDepositAccountRepository.class,
            SpringDataDepositOutboxRepository.class})
    static class TestApplication {}

    @Autowired private DepositAccountPersistencePort accounts;
    @Autowired private SpringDataDepositAccountRepository accountRepository;
    @Autowired private SpringDataDepositOutboxRepository outboxRepository;
    @Autowired private OutboxPort outbox;
    @Autowired private PlatformTransactionManager transactionManager;

    private DepositAccountMappingPort mapping;
    private MasterDataQueryPort masterData;
    private JournalPostingPort journal;
    private DepositService service;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        accountRepository.deleteAll();
        mapping = mock(DepositAccountMappingPort.class);
        masterData = mock(MasterDataQueryPort.class);
        journal = mock(JournalPostingPort.class);
        when(mapping.resolveInitialDepositAccounts(any()))
                .thenReturn(new DepositAccountMappingPort.InitialDepositAccounts("10100", "20200"));
        when(masterData.findAccountSubject(any()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        service = serviceWith(outbox);
        DepositAccount account = new DepositAccount();
        account.setAccountNumber("DEP-TX-1");
        account.setCustomerCode("CUST-1");
        account.setProductCode("PROD-1");
        account.setCurrencyCode("KRW");
        account.setInterestRate(new BigDecimal("0.01"));
        account.setStatus(DepositStatus.ACTIVE);
        account.setOpenedAt(LocalDate.now());
        account.setBalance(new BigDecimal("100.00"));
        accounts.save(account);
    }

    @Test
    void committedBalanceChangesHaveDistinctPendingBalancedOutboxes() {
        service.deposit("DEP-TX-1", new BigDecimal("20.25"));
        service.withdraw("DEP-TX-1", new BigDecimal("10.10"));

        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("110.15");
        List<DepositOutboxEntity> rows = outboxRepository.findAll();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(DepositOutboxEntity::getLineageSourceId).doesNotHaveDuplicates();
        for (DepositOutboxEntity row : rows) {
            assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(row.getIdempotencyKey()).isEqualTo("DEPOSIT_TRANSACTION:" + row.getLineageSourceId());
            JournalOutboxEvent event = outbox.findJournalEventById(row.getEventId()).orElseThrow();
            assertThat(event.getCommand().lineageSourceId()).isEqualTo(row.getLineageSourceId());
            assertThat(event.getCommand().lines()).hasSize(2);
            assertThat(event.getCommand().lines().get(0).amount())
                    .isEqualByComparingTo(event.getCommand().lines().get(1).amount());
        }
        verify(journal, never()).createDraftEntry(any());
    }

    @Test
    void outboxWriteFailureRollsBackAccountBalance() {
        OutboxPort failingOutbox = mock(OutboxPort.class, delegatesTo(outbox));
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(failingOutbox).saveJournalEvent(any(JournalOutboxEvent.class));

        assertThatThrownBy(() -> serviceWith(failingOutbox).deposit("DEP-TX-1", new BigDecimal("50.00")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("outbox unavailable");

        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void withdrawalOutboxFailureRollsBackAccountBalance() {
        OutboxPort failingOutbox = mock(OutboxPort.class, delegatesTo(outbox));
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(failingOutbox).saveJournalEvent(any(JournalOutboxEvent.class));

        assertThatThrownBy(() -> serviceWith(failingOutbox).withdraw("DEP-TX-1", new BigDecimal("30.00")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("outbox unavailable");

        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void rejectedWithdrawalLeavesNoOutbox() {
        assertThatThrownBy(() -> service.withdraw("DEP-TX-1", new BigDecimal("100.01")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("잔액이 부족");

        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void foreignCurrencyDepositAndWithdrawalLeaveBalanceAndOutboxUntouched() {
        DepositAccount foreignCurrencyAccount = accounts.findByAccountNumber("DEP-TX-1").orElseThrow();
        foreignCurrencyAccount.setCurrencyCode("USD");
        accounts.save(foreignCurrencyAccount);

        assertThatThrownBy(() -> service.deposit("DEP-TX-1", new BigDecimal("10.00")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(outboxRepository.count()).isZero();

        assertThatThrownBy(() -> service.withdraw("DEP-TX-1", new BigDecimal("10.00")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void subCentAmountsAreRejectedBeforeBalanceOrOutboxChanges() {
        assertThatThrownBy(() -> service.deposit("DEP-TX-1", new BigDecimal("0.0001")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.withdraw("DEP-TX-1", new BigDecimal("0.0001")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void exactTrailingZeroAmountUsesCentPrecisionInJournal() {
        service.deposit("DEP-TX-1", new BigDecimal("1.0000"));

        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("101.00");
        JournalOutboxEvent event = outbox.findJournalEventById(outboxRepository.findAll().get(0).getEventId())
                .orElseThrow();
        assertThat(event.getCommand().lines()).allSatisfy(line -> {
            assertThat(line.amount()).isEqualByComparingTo("1.00");
            assertThat(line.amount().scale()).isEqualTo(2);
        });
    }

    @Test
    void optimisticConflictRetriesInFreshTransactionWithoutDuplicateOutbox() {
        DepositAccountPersistencePort conflictingAccounts = mock(DepositAccountPersistencePort.class, delegatesTo(accounts));
        AtomicInteger saves = new AtomicInteger();
        doAnswer(invocation -> {
            if (saves.incrementAndGet() == 1) {
                throw new OptimisticLockingFailureException("stale account version") {};
            }
            return accounts.save(invocation.getArgument(0));
        }).when(conflictingAccounts).save(any(DepositAccount.class));
        DepositService retryingService = new DepositService(conflictingAccounts, mapping, masterData,
                journal, outbox, new JournalOutboxRelayService(outbox, journal), transactionManager);

        retryingService.deposit("DEP-TX-1", new BigDecimal("5.00"));

        assertThat(saves).hasValue(2);
        verify(conflictingAccounts, times(2)).findByAccountNumber("DEP-TX-1");
        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("105.00");
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void conflictAfterOutboxInsertRollsBackAttemptBeforeRetry() {
        OutboxPort conflictingOutbox = mock(OutboxPort.class, delegatesTo(outbox));
        List<JournalOutboxEvent> attemptedEvents = new ArrayList<>();
        doAnswer(invocation -> {
            JournalOutboxEvent event = invocation.getArgument(0);
            attemptedEvents.add(event);
            JournalOutboxEvent saved = outbox.saveJournalEvent(event);
            if (attemptedEvents.size() == 1) {
                throw new OptimisticLockingFailureException("conflict after outbox insert") {};
            }
            return saved;
        }).when(conflictingOutbox).saveJournalEvent(any(JournalOutboxEvent.class));

        serviceWith(conflictingOutbox).deposit("DEP-TX-1", new BigDecimal("5.00"));

        assertThat(attemptedEvents).hasSize(2);
        assertThat(attemptedEvents.get(0).getLineageSourceId())
                .isEqualTo(attemptedEvents.get(1).getLineageSourceId());
        assertThat(attemptedEvents.get(0).getEventId())
                .isNotEqualTo(attemptedEvents.get(1).getEventId());
        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("105.00");
        assertThat(outboxRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.findByIdempotencyKey(attemptedEvents.get(1).getIdempotencyKey()))
                .isPresent();
    }

    @Test
    void closedPeriodRelayRejectionKeepsCommittedBalanceAndPendingEventForRecovery() {
        service.withdraw("DEP-TX-1", new BigDecimal("25.00"));
        DepositOutboxEntity row = outboxRepository.findAll().get(0);
        when(journal.createDraftEntry(any())).thenThrow(new IllegalStateException("closed accounting period"))
                .thenReturn(new JournalPostingResult(10L, "SLIP-10", "DRAFT"));
        JournalOutboxRelayService relay = new JournalOutboxRelayService(outbox, journal);

        assertThat(relay.publishPendingJournalEvents()).isZero();
        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("75.00");
        assertThat(outboxRepository.findById(row.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxRepository.findById(row.getId()).orElseThrow().getRetryCount()).isEqualTo(1);

        assertThat(relay.publishPendingJournalEvents()).isEqualTo(1);
        assertThat(outboxRepository.findById(row.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(accounts.findByAccountNumber("DEP-TX-1").orElseThrow().getBalance())
                .isEqualByComparingTo("75.00");
    }

    private DepositService serviceWith(OutboxPort selectedOutbox) {
        return new DepositService(accounts, mapping, masterData, journal, selectedOutbox,
                new JournalOutboxRelayService(selectedOutbox, journal), transactionManager);
    }
}
