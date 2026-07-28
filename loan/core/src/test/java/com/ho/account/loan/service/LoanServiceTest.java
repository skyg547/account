package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.loan.application.port.in.LoanUseCase;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.AccountReference;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.LoanReferenceSnapshot;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.RecalculationRun;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanPersistencePort persistencePort;
    @Mock private EIRCalculator eirCalculator;
    @Mock private LoanReferenceDataPort referenceDataPort;
    @Mock private LoanJournalPort journalPort;

    private LoanService service;
    private LoanAccountingProperties accountingProperties;

    @BeforeEach
    void setUp() {
        accountingProperties = new LoanAccountingProperties();
        accountingProperties.setCashAccountCode("101999");
        accountingProperties.setLoanReceivableAccountCode("131999");
        accountingProperties.setDeferredAssetAccountCode("171999");
        accountingProperties.setRecognizedIncomeAccountCode("401999");
        service = new LoanService(
                persistencePort,
                eirCalculator,
                referenceDataPort,
                accountingProperties,
                journalPort);
    }

    @Test
    @DisplayName("대출 생성은 값 참조만 저장하고 기준정보 이름은 transient 설명으로 연결한다.")
    void createLoanValidatesReferencesWithoutMasterDataEntities() {
        Loan loan = pendingLoan();
        when(referenceDataPort.requireLoanReferences(100L, "KRW", loan.getDisbursalDate()))
                .thenReturn(new LoanReferenceSnapshot(100L, "차주 A", "KRW"));
        when(persistencePort.saveLoan(loan)).thenReturn(loan);

        Loan created = service.createLoan(loan);

        assertThat(created.getBusinessPartnerId()).isEqualTo(100L);
        assertThat(created.getBusinessPartnerName()).isEqualTo("차주 A");
        assertThat(created.getStatus()).isEqualTo(Loan.LoanStatus.PENDING_DISBURSEMENT);
    }

    @Test
    @DisplayName("대출 실행은 계정 검증 후 균형 전표를 전기하고 단일 실행 상태를 활성화한다.")
    void disburseLoanPostsBalancedJournalAndActivatesLoan() {
        Loan loan = pendingLoan();
        LocalDate date = loan.getDisbursalDate();
        when(persistencePort.findLoanForUpdate(1L)).thenReturn(Optional.of(loan));
        when(persistencePort.existsDisbursal(1L)).thenReturn(false);
        when(referenceDataPort.requireAccount("101999", date))
                .thenReturn(new AccountReference("101999", "Cash"));
        when(referenceDataPort.requireAccount("131999", date))
                .thenReturn(new AccountReference("131999", "Loan receivable"));
        when(journalPort.post(any())).thenReturn(new LoanJournalPort.PostedJournal(77L, "JE-LOAN-1"));
        when(persistencePort.saveLoan(loan)).thenReturn(loan);
        when(persistencePort.saveDisbursal(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoanDisbursal disbursal = service.disburseLoan(
                1L, date, loan.getPrincipalAmount(), "loan-user");

        ArgumentCaptor<LoanJournalPort.LoanJournalCommand> commandCaptor =
                ArgumentCaptor.forClass(LoanJournalPort.LoanJournalCommand.class);
        verify(journalPort).post(commandCaptor.capture());
        assertThat(commandCaptor.getValue().lines())
                .extracting(LoanJournalPort.LoanJournalLine::accountCode)
                .containsExactly("131999", "101999");
        assertThat(loan.getStatus()).isEqualTo(Loan.LoanStatus.ACTIVE);
        assertThat(disbursal.getJournalEntryId()).isEqualTo(77L);
    }

    @Test
    void duplicateDisbursalFailsBeforeJournalPosting() {
        when(persistencePort.findLoanForUpdate(1L)).thenReturn(Optional.of(pendingLoan()));
        when(persistencePort.existsDisbursal(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.disburseLoan(
                1L,
                LocalDate.of(2026, 5, 10),
                new BigDecimal("5000000.00"),
                "loan-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been disbursed");
        verify(journalPort, never()).post(any());
    }

    @Test
    @DisplayName("중도상환은 원래 약정 원금을 보존하고 현재 잔액과 이벤트 전표 계보를 갱신한다.")
    void recalculateLoanPreservesOriginalPrincipalAndCopiesJournalLineage() {
        Loan loan = activeLoan();
        LocalDate date = LocalDate.of(2026, 8, 10);
        BigDecimal newOutstanding = new BigDecimal("4000000.00");
        when(persistencePort.findLoanForUpdate(1L)).thenReturn(Optional.of(loan));
        when(persistencePort.findDeferredItems(loan)).thenReturn(List.of());
        when(eirCalculator.calculateEIR(
                eq(loan), eq(newOutstanding), eq(loan.getMaturityDate()), anyList()))
                .thenReturn(new BigDecimal("0.0400"));
        when(referenceDataPort.requireAccount("101999", date))
                .thenReturn(new AccountReference("101999", "Cash"));
        when(referenceDataPort.requireAccount("131999", date))
                .thenReturn(new AccountReference("131999", "Loan receivable"));
        when(persistencePort.findSchedulesFrom(loan, date)).thenReturn(List.of());
        when(persistencePort.saveSchedules(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.findSchedulesOrdered(loan)).thenReturn(List.of());
        when(persistencePort.saveLoan(loan)).thenReturn(loan);
        when(journalPort.post(any())).thenReturn(new LoanJournalPort.PostedJournal(88L, "JE-ADJ-88"));
        when(persistencePort.saveRecalculationRun(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.saveLoanEvent(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RecalculationRun run = service.recalculateLoan(
                1L,
                date,
                RecalculationRun.RecalculationReason.EARLY_REPAYMENT,
                "loan-user",
                Optional.of(newOutstanding),
                Optional.empty());

        assertThat(loan.getPrincipalAmount()).isEqualByComparingTo("5000000.00");
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo(newOutstanding);
        assertThat(run.getAdjustmentJournalEntryId()).isEqualTo(88L);
        ArgumentCaptor<LoanEvent> eventCaptor = ArgumentCaptor.forClass(LoanEvent.class);
        verify(persistencePort).saveLoanEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getRelatedJournalEntrySlipNo()).isEqualTo("JE-ADJ-88");
    }

    @Test
    void defaultEventChangesStatusWithoutForcingRecalculationEnumConversion() {
        Loan loan = activeLoan();
        when(persistencePort.findLoanForUpdate(1L)).thenReturn(Optional.of(loan));
        when(persistencePort.saveLoan(loan)).thenReturn(loan);
        when(persistencePort.saveLoanEvent(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoanUseCase.LoanEventResult result = service.processLoanEvent(
                1L,
                LoanEvent.EventType.DEFAULT,
                LocalDate.of(2026, 9, 1),
                "Borrower defaulted",
                "risk-user",
                Optional.empty(),
                Optional.empty());

        assertThat(loan.getStatus()).isEqualTo(Loan.LoanStatus.DEFAULTED);
        assertThat(result.recalculationRun()).isEmpty();
        assertThat(result.event().getEventType()).isEqualTo(LoanEvent.EventType.DEFAULT);
        verify(eirCalculator, never()).calculateEIR(any(), anyList());
    }

    @Test
    void configuredDeferredAccountCodesAreValidatedAndPersisted() {
        DeferredItemType input = DeferredItemType.create(
                "FEE",
                "Fee",
                null,
                DeferredItemType.DeferralMethod.EIR_METHOD,
                DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW,
                "118111",
                "410111",
                true,
                "loan-user");
        when(persistencePort.findDeferredItemType("FEE")).thenReturn(Optional.empty());
        when(referenceDataPort.requireAccount(eq("118111"), any(LocalDate.class)))
                .thenReturn(new AccountReference("118111", "Deferred asset"));
        when(referenceDataPort.requireAccount(eq("410111"), any(LocalDate.class)))
                .thenReturn(new AccountReference("410111", "Fee income"));
        when(persistencePort.saveDeferredItemType(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DeferredItemType saved = service.createDeferredItemType(input);

        assertThat(saved.getDeferredAssetAccountCode()).isEqualTo("118111");
        assertThat(saved.getRecognizedIncomeAccountCode()).isEqualTo("410111");
    }

    @Test
    void missingAccountingConfigurationFailsBeforeStateChangeAndJournal() {
        Loan loan = pendingLoan();
        accountingProperties.setCashAccountCode(null);
        when(persistencePort.findLoanForUpdate(1L)).thenReturn(Optional.of(loan));
        when(persistencePort.existsDisbursal(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.disburseLoan(
                1L, loan.getDisbursalDate(), loan.getPrincipalAmount(), "loan-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cash-account-code");
        assertThat(loan.getStatus()).isEqualTo(Loan.LoanStatus.PENDING_DISBURSEMENT);
        verify(journalPort, never()).post(any());
    }

    private Loan pendingLoan() {
        Loan loan = Loan.create(
                "LN-2026-001",
                100L,
                "KRW",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("5000000.00"),
                new BigDecimal("0.0450"),
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2027, 5, 10),
                Loan.PaymentFrequency.MONTHLY,
                "loan-user");
        loan.setId(1L);
        return loan;
    }

    private Loan activeLoan() {
        Loan loan = pendingLoan();
        loan.activateAfterDisbursal(
                loan.getDisbursalDate(), loan.getPrincipalAmount(), "loan-user");
        return loan;
    }
}
