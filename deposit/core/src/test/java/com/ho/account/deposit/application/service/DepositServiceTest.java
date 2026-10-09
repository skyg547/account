package com.ho.account.deposit.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.outbox.JournalOutboxEvent;
import com.ho.account.contracts.outbox.OutboxEventPublisher;
import com.ho.account.contracts.outbox.OutboxPort;
import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import com.ho.account.deposit.application.port.out.DepositAccountMappingPort;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock
    private DepositAccountPersistencePort depositAccountPersistencePort;
    @Mock
    private DepositAccountMappingPort depositAccountMappingPort;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;
    @Mock
    private JournalPostingPort journalPostingPort;
    @Mock
    private OutboxPort outboxPort;
    @Mock
    private OutboxEventPublisher outboxEventPublisher;
    @Mock
    private PlatformTransactionManager transactionManager;

    private DepositService service;

    @BeforeEach
    void setUp() {
        service = new DepositService(
                depositAccountPersistencePort,
                depositAccountMappingPort,
                masterDataQueryPort,
                journalPostingPort);
    }

    @Test
    @DisplayName("초기입금이 있으면 계좌 저장 후 초기입금 전표를 생성한다")
    void openAccountPostsInitialDepositJournal() {
        OpenAccountUseCase.OpenAccountCommand command = command(new BigDecimal("1000.00"));

        when(depositAccountPersistencePort.save(any(DepositAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(depositAccountMappingPort.resolveInitialDepositAccounts(any(DepositAccount.class)))
                .thenReturn(new DepositAccountMappingPort.InitialDepositAccounts("CASH-001", "DEP-201"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(10L, "SLIP-10", "DRAFT"));

        String accountNumber = service.openAccount(command);

        ArgumentCaptor<DepositAccount> accountCaptor = ArgumentCaptor.forClass(DepositAccount.class);
        verify(depositAccountPersistencePort).save(accountCaptor.capture());
        DepositAccount savedAccount = accountCaptor.getValue();

        assertThat(accountNumber).isEqualTo(savedAccount.getAccountNumber());
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("1000.00");

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());
        JournalEntryCommand journalCommand = commandCaptor.getValue();

        assertThat(journalCommand.entryType()).isEqualTo("DEPOSIT_INITIAL_DEPOSIT");
        assertThat(journalCommand.currencyCode()).isEqualTo("KRW");
        assertThat(journalCommand.createdBy()).isEqualTo("SYSTEM");
        assertThat(journalCommand.lineageSourceId()).isEqualTo(accountNumber);
        assertThat(journalCommand.lines()).extracting("accountCode")
                .containsExactly("CASH-001", "DEP-201");
    }

    @Test
    @DisplayName("초기입금이 없으면 계좌만 저장하고 전표는 생성하지 않는다")
    void openAccountWithoutInitialDepositDoesNotPostJournal() {
        when(depositAccountPersistencePort.save(any(DepositAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.openAccount(command(BigDecimal.ZERO));

        verify(journalPostingPort, never()).createDraftEntry(any());
        verify(depositAccountMappingPort, never()).resolveInitialDepositAccounts(any());
    }


    @Test
    @DisplayName("계좌 개설 command는 필수 코드와 음수 금액을 먼저 거부한다")
    void openAccountCommandRejectsInvalidInput() {
        assertThatThrownBy(() -> new OpenAccountUseCase.OpenAccountCommand(
                " ",
                "DMD-001",
                "KRW",
                BigDecimal.ZERO,
                new BigDecimal("0.02500000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerCode");

        assertThatThrownBy(() -> new OpenAccountUseCase.OpenAccountCommand(
                "CUST-001",
                "DMD-001",
                "KRW",
                new BigDecimal("-1.00"),
                new BigDecimal("0.02500000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initialDeposit");

        assertThatThrownBy(() -> new OpenAccountUseCase.OpenAccountCommand(
                "CUST-001",
                "DMD-001",
                "KRW",
                BigDecimal.ZERO,
                new BigDecimal("-0.0001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interestRate");
    }

    @Test
    @DisplayName("입출금은 서로 다른 거래 계보로 균형 잡힌 역방향 분개를 outbox에 저장한다")
    void depositAndWithdrawalCreateBalancedJournalOutbox() {
        service = transactionalService();
        when(depositAccountPersistencePort.findByAccountNumber("DEP-1"))
                .thenAnswer(invocation -> Optional.of(activeAccount("100.00")));
        when(depositAccountPersistencePort.save(any(DepositAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubJournalAccounts();

        service.deposit("DEP-1", new BigDecimal("25.25"));
        service.withdraw("DEP-1", new BigDecimal("10.10"));

        ArgumentCaptor<JournalOutboxEvent> captor = ArgumentCaptor.forClass(JournalOutboxEvent.class);
        verify(outboxPort, times(2)).saveJournalEvent(captor.capture());
        List<JournalOutboxEvent> events = captor.getAllValues();
        assertThat(events).extracting(JournalOutboxEvent::getLineageSourceId).doesNotHaveDuplicates();
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getLineageSourceType()).isEqualTo("DEPOSIT_TRANSACTION");
            assertThat(event.getIdempotencyKey()).isEqualTo("DEPOSIT_TRANSACTION:" + event.getLineageSourceId());
            assertThat(event.getCommand().lineageSourceId()).isEqualTo(event.getLineageSourceId());
            assertThat(event.getCommand().lines()).hasSize(2);
            assertThat(event.getCommand().lines().get(0).amount())
                    .isEqualByComparingTo(event.getCommand().lines().get(1).amount());
        });
        assertJournal(events.get(0), "DEPOSIT_DEPOSIT", "DEBIT", "CASH-001", "CREDIT", "DEP-201", "25.25");
        assertJournal(events.get(1), "DEPOSIT_WITHDRAWAL", "DEBIT", "DEP-201", "CREDIT", "CASH-001", "10.10");
        verify(outboxEventPublisher, never()).publish(any());
        verify(journalPostingPort, never()).createDraftEntry(any());
    }

    @Test
    @DisplayName("충돌 후 출금은 최신 잔액을 재검증하고 실패한 시도는 outbox를 남기지 않는다")
    void withdrawalRetryRechecksLatestBalance() {
        service = transactionalService();
        when(depositAccountPersistencePort.findByAccountNumber("DEP-1"))
                .thenReturn(Optional.of(activeAccount("100.00")), Optional.of(activeAccount("40.00")));
        when(depositAccountPersistencePort.save(any(DepositAccount.class)))
                .thenThrow(new OptimisticLockingFailureException("conflict") {});

        assertThatThrownBy(() -> service.withdraw("DEP-1", new BigDecimal("60.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔액이 부족");

        verify(depositAccountPersistencePort, times(2)).findByAccountNumber("DEP-1");
        verify(depositAccountPersistencePort).save(any(DepositAccount.class));
        verify(outboxPort, never()).saveJournalEvent(any(JournalOutboxEvent.class));
    }

    private DepositService transactionalService() {
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        return new DepositService(depositAccountPersistencePort, depositAccountMappingPort,
                masterDataQueryPort, journalPostingPort, outboxPort, outboxEventPublisher, transactionManager);
    }

    private void stubJournalAccounts() {
        when(depositAccountMappingPort.resolveInitialDepositAccounts(any(DepositAccount.class)))
                .thenReturn(new DepositAccountMappingPort.InitialDepositAccounts("CASH-001", "DEP-201"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
    }

    private DepositAccount activeAccount(String balance) {
        DepositAccount account = new DepositAccount();
        account.setAccountNumber("DEP-1");
        account.setCustomerCode("CUST-001");
        account.setCurrencyCode("KRW");
        account.setStatus(com.ho.account.deposit.domain.DepositStatus.ACTIVE);
        account.setOpenedAt(java.time.LocalDate.now());
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    private void assertJournal(JournalOutboxEvent event, String entryType, String firstSide,
                               String firstAccount, String secondSide, String secondAccount, String amount) {
        JournalEntryCommand command = event.getCommand();
        assertThat(command.entryType()).isEqualTo(entryType);
        assertThat(command.lines().get(0).drcrType()).isEqualTo(firstSide);
        assertThat(command.lines().get(0).accountCode()).isEqualTo(firstAccount);
        assertThat(command.lines().get(0).amount()).isEqualByComparingTo(amount);
        assertThat(command.lines().get(1).drcrType()).isEqualTo(secondSide);
        assertThat(command.lines().get(1).accountCode()).isEqualTo(secondAccount);
        assertThat(command.lines().get(1).amount()).isEqualByComparingTo(amount);
    }
    private OpenAccountUseCase.OpenAccountCommand command(BigDecimal initialDeposit) {
        return new OpenAccountUseCase.OpenAccountCommand(
                "CUST-001",
                "DMD-001",
                "KRW",
                initialDeposit,
                new BigDecimal("0.02500000"));
    }
}
