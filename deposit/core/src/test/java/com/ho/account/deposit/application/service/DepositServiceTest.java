package com.ho.account.deposit.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
    private OpenAccountUseCase.OpenAccountCommand command(BigDecimal initialDeposit) {
        return new OpenAccountUseCase.OpenAccountCommand(
                "CUST-001",
                "DMD-001",
                "KRW",
                initialDeposit,
                new BigDecimal("0.02500000"));
    }
}
