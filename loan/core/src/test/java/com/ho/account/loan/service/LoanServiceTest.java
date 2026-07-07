package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.RecalculationRun;
import com.ho.account.loan.dto.LoanRequestDto;
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
    @DisplayName("대출 생성은 Loan 소유 기준정보 포트를 통해 거래처와 통화를 검증한다.")
    void createLoanResolvesReferencesThroughLoanPort() {
        LoanRequestDto request = loanRequest();
        when(persistencePort.saveLoan(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan created = service.createLoan(request.toEntity());

        verify(referenceDataPort).attachValidatedLoanReferences(created);
        assertThat(created.getInitialEIR()).isEqualByComparingTo("0.0450");
        assertThat(created.getCurrentEIR()).isEqualByComparingTo("0.0450");
        assertThat(created.getStatus()).isEqualTo(Loan.LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("대출 실행은 LoanJournalPort에 균형 전표 명령을 보내고 전표 참조값만 보관한다.")
    void disburseLoanPostsJournalThroughLoanPort() {
        Loan loan = loan();
        when(persistencePort.findLoan(1L)).thenReturn(Optional.of(loan));
        when(referenceDataPort.requireAccountCode("101999")).thenReturn("101999");
        when(referenceDataPort.requireAccountCode("131999")).thenReturn("131999");
        when(journalPort.post(any())).thenReturn(new LoanJournalPort.PostedJournal(77L, "JE-LOAN-1"));
        when(persistencePort.saveDisbursal(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoanDisbursal disbursal = service.disburseLoan(
                1L,
                LocalDate.of(2026, 5, 10),
                new BigDecimal("1000000.00"),
                "loan-user");

        ArgumentCaptor<LoanJournalPort.LoanJournalCommand> commandCaptor =
                ArgumentCaptor.forClass(LoanJournalPort.LoanJournalCommand.class);
        verify(journalPort).post(commandCaptor.capture());
        LoanJournalPort.LoanJournalCommand command = commandCaptor.getValue();
        assertThat(command.lineageSourceType()).isEqualTo("LOAN_DISBURSAL");
        assertThat(command.currencyCode()).isEqualTo("KRW");
        assertThat(command.lines()).extracting(LoanJournalPort.LoanJournalLine::accountCode)
                .containsExactly("131999", "101999");
        assertThat(disbursal.getJournalEntryId()).isEqualTo(77L);
        assertThat(disbursal.getJournalEntrySlipNo()).isEqualTo("JE-LOAN-1");
    }


    @Test
    @DisplayName("중도상환 재계산은 조정 전표 참조값을 이벤트 이력에도 남긴다.")
    void recalculateLoanCopiesAdjustmentJournalReferenceToLoanEvent() {
        Loan loan = loan();
        loan.setCurrentEIR(new BigDecimal("0.0450"));
        LocalDate recalculationDate = LocalDate.of(2026, 8, 10);

        when(persistencePort.findLoan(1L)).thenReturn(Optional.of(loan));
        when(persistencePort.findDeferredItems(loan)).thenReturn(List.of());
        when(eirCalculator.calculateEIR(loan, List.of())).thenReturn(new BigDecimal("0.0400"));
        when(persistencePort.saveLoan(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.findSchedulesFrom(loan, recalculationDate)).thenReturn(List.of());
        when(persistencePort.findSchedulesOrdered(loan)).thenReturn(List.of());
        when(referenceDataPort.requireAccountCode("101999")).thenReturn("101999");
        when(referenceDataPort.requireAccountCode("131999")).thenReturn("131999");
        when(journalPort.post(any())).thenReturn(new LoanJournalPort.PostedJournal(88L, "JE-ADJ-88"));
        when(persistencePort.saveRecalculationRun(any(RecalculationRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.saveLoanEvent(any(LoanEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recalculateLoan(
                1L,
                recalculationDate,
                RecalculationRun.RecalculationReason.EARLY_REPAYMENT,
                "loan-user",
                Optional.of(new BigDecimal("4000000.00")),
                Optional.empty());

        ArgumentCaptor<LoanEvent> eventCaptor = ArgumentCaptor.forClass(LoanEvent.class);
        verify(persistencePort).saveLoanEvent(eventCaptor.capture());
        LoanEvent event = eventCaptor.getValue();
        assertThat(event.getRelatedJournalEntryId()).isEqualTo(88L);
        assertThat(event.getRelatedJournalEntrySlipNo()).isEqualTo("JE-ADJ-88");
    }

    @Test
    @DisplayName("대출 자동전표 계정 설정이 없으면 숨은 기본값 없이 실패한다.")
    void disburseLoanRejectsMissingAccountingConfiguration() {
        accountingProperties.setCashAccountCode(null);
        when(persistencePort.findLoan(1L)).thenReturn(Optional.of(loan()));

        assertThatThrownBy(() -> service.disburseLoan(
                1L,
                LocalDate.of(2026, 5, 10),
                new BigDecimal("1000000.00"),
                "loan-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("account.loan.accounting.cash-account-code");
        verify(journalPort, never()).post(any());
    }

    private LoanRequestDto loanRequest() {
        LoanRequestDto request = new LoanRequestDto();
        request.setLoanNumber("LN-2026-DTO");
        request.setBusinessPartnerId(100L);
        request.setCurrencyCode("KRW");
        request.setLoanType(Loan.LoanType.TERM_LOAN);
        request.setPrincipalAmount(new BigDecimal("5000000.00"));
        request.setInterestRate(new BigDecimal("0.0450"));
        request.setDisbursalDate(LocalDate.of(2026, 5, 10));
        request.setMaturityDate(LocalDate.of(2027, 5, 10));
        request.setPaymentFrequency(Loan.PaymentFrequency.MONTHLY);
        return request;
    }

    private Loan loan() {
        Loan loan = loanRequest().toEntity();
        loan.setId(1L);
        return loan;
    }
}
