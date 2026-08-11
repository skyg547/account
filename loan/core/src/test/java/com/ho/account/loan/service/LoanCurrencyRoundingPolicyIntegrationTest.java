package com.ho.account.loan.service;

import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanJournalPort.PostedJournal;
import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.AccountReference;
import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanCurrencyRoundingPolicyIntegrationTest {

    @Mock
    private LoanPersistencePort persistencePort;
    @Mock
    private EIRCalculator eirCalculator;
    @Mock
    private LoanReferenceDataPort referenceDataPort;
    @Mock
    private LoanJournalPort journalPort;

    private LoanService loanService;
    private LoanAccountingProperties accountingProperties;

    @BeforeEach
    void setUp() {
        accountingProperties = new LoanAccountingProperties();
        accountingProperties.setCashAccountCode("10100");
        accountingProperties.setLoanReceivableAccountCode("12100");
        accountingProperties.setDeferredAssetAccountCode("17100");
        accountingProperties.setRecognizedIncomeAccountCode("40100");

        loanService = new LoanService(
                persistencePort,
                eirCalculator,
                referenceDataPort,
                accountingProperties,
                journalPort);

        when(referenceDataPort.requireAccount(anyString(), any(LocalDate.class)))
                .thenAnswer(i -> new AccountReference(i.getArgument(0), "AccountName"));
    }

    @Test
    @DisplayName("KRW 대출 이연 항목 생성 시 1원 미만 절사(FLOOR) 규칙이 적용된다")
    void testKrwDeferredItemRounding() {
        // given
        Loan krwLoan = Loan.create(
                "LN-2026-001",
                100L,
                "KRW",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("1000000"),
                new BigDecimal("0.05"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                Loan.PaymentFrequency.MONTHLY,
                "ADMIN"
        );
        krwLoan.setId(1L);
        krwLoan.activateAfterDisbursal(LocalDate.of(2026, 1, 1), new BigDecimal("1000000"), "ADMIN");

        DeferredItemType itemType = DeferredItemType.create(
                "FEE", "Loan Fee", "Fee desc",
                DeferredItemType.DeferralMethod.STRAIGHT_LINE,
                DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW,
                "17100", "40100", true, "ADMIN");
        itemType.setId(10L);

        when(persistencePort.findLoanForUpdate(1L)).thenReturn(Optional.of(krwLoan));
        when(persistencePort.findDeferredItemType(10L)).thenReturn(Optional.of(itemType));

        PostedJournal mockJournal = new PostedJournal(200L, "SLIP-DEF-01");
        when(journalPort.post(any())).thenReturn(mockJournal);
        when(persistencePort.saveDeferredItem(any())).thenAnswer(i -> i.getArgument(0));

        // when
        DeferredItem item = loanService.createDeferredItem(
                1L, 10L, new BigDecimal("15000.75"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "ADMIN");

        // then
        assertThat(item.getAmount()).isEqualTo(new BigDecimal("15000"));

        ArgumentCaptor<LoanJournalPort.LoanJournalCommand> captor = ArgumentCaptor.forClass(LoanJournalPort.LoanJournalCommand.class);
        verify(journalPort).post(captor.capture());
        assertThat(captor.getValue().currencyCode()).isEqualTo("KRW");
        assertThat(captor.getValue().lines().get(0).amount()).isEqualTo(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("USD 대출 이연 항목 생성 시 Cents 단위 반올림(HALF_UP) 규칙이 적용된다")
    void testUsdDeferredItemRounding() {
        // given
        Loan usdLoan = Loan.create(
                "LN-2026-002",
                200L,
                "USD",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("5000"),
                new BigDecimal("0.04"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                Loan.PaymentFrequency.MONTHLY,
                "ADMIN"
        );
        usdLoan.setId(2L);
        usdLoan.activateAfterDisbursal(LocalDate.of(2026, 1, 1), new BigDecimal("5000"), "ADMIN");

        DeferredItemType itemType = DeferredItemType.create(
                "FEE_USD", "Loan Fee USD", "Fee desc",
                DeferredItemType.DeferralMethod.STRAIGHT_LINE,
                DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW,
                "17100", "40100", true, "ADMIN");
        itemType.setId(20L);

        when(persistencePort.findLoanForUpdate(2L)).thenReturn(Optional.of(usdLoan));
        when(persistencePort.findDeferredItemType(20L)).thenReturn(Optional.of(itemType));

        PostedJournal mockJournal = new PostedJournal(201L, "SLIP-DEF-02");
        when(journalPort.post(any())).thenReturn(mockJournal);
        when(persistencePort.saveDeferredItem(any())).thenAnswer(i -> i.getArgument(0));

        // when
        DeferredItem item = loanService.createDeferredItem(
                2L, 20L, new BigDecimal("150.125"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "ADMIN");

        // then
        assertThat(item.getAmount()).isEqualTo(new BigDecimal("150.13"));

        ArgumentCaptor<LoanJournalPort.LoanJournalCommand> captor = ArgumentCaptor.forClass(LoanJournalPort.LoanJournalCommand.class);
        verify(journalPort).post(captor.capture());
        assertThat(captor.getValue().currencyCode()).isEqualTo("USD");
        assertThat(captor.getValue().lines().get(0).amount()).isEqualTo(new BigDecimal("150.13"));
    }
}
