package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxValuationServiceTest {

    @Mock
    private FxExchangeRateLookupPort fxExchangeRateLookupPort;
    @Mock
    private ClosingJournalEntryPort closingJournalEntryPort;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;

    private ClosingAccountingProperties accountingProperties;
    private FxValuationService service;

    @BeforeEach
    void setUp() {
        accountingProperties = new ClosingAccountingProperties();
        accountingProperties.setFxValuationReportingCurrencyCode("USD");
        accountingProperties.setFxTranslationGainAccountCode("720100");
        accountingProperties.setFxTranslationLossAccountCode("920100");
        service = new FxValuationService(
                fxExchangeRateLookupPort,
                closingJournalEntryPort,
                accountingProperties,
                masterDataQueryPort);
    }

    @Test
    @DisplayName("FX 평가는 정책 보고통화와 장부 기준통화 잔액으로 차액 전표를 생성한다")
    void processFxValuationUsesPolicyCurrencyAndBaseEndingBalance() {
        accountingProperties.setAutoPostAdjustments(true);
        LocalDate valuationDate = LocalDate.of(2026, 5, 31);
        FxValuationBalance balance = balance("113000", "EUR", "100.00", "110.00");

        when(fxExchangeRateLookupPort.findRate("EUR", "USD", valuationDate))
                .thenReturn(Optional.of(new BigDecimal("1.20000000")));
        when(masterDataQueryPort.findAccountSubjectAt("113000", valuationDate))
                .thenReturn(Optional.of(new AccountSubjectRef("113000", "EUR Cash", false, false, "DEBIT")));
        when(closingJournalEntryPort.createDraftAdjustment(any(ClosingJournalEntryCommand.class)))
                .thenReturn(new ClosingJournalEntryResult(501L, "FXV2026053177ABC"));

        service.processFxValuationForAccount(balance, valuationDate, 77L);

        ArgumentCaptor<ClosingJournalEntryCommand> captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        ClosingJournalEntryCommand command = captor.getValue();

        assertThat(command.currencyCode()).isEqualTo("USD");
        assertThat(command.lineageSourceId()).isEqualTo("77");
        assertThat(command.lines()).hasSize(2);
        assertThat(command.lines().get(0).side()).isEqualTo(ClosingJournalSide.DEBIT);
        assertThat(command.lines().get(0).accountCode()).isEqualTo("113000");
        assertThat(command.lines().get(0).amount()).isEqualByComparingTo("10.00");
        assertThat(command.lines().get(1).side()).isEqualTo(ClosingJournalSide.CREDIT);
        assertThat(command.lines().get(1).accountCode()).isEqualTo("720100");
        assertThat(command.lines().get(1).amount()).isEqualByComparingTo("10.00");
        verify(closingJournalEntryPort).approveAndPost(501L, "SYSTEM");
    }

    @Test
    @DisplayName("대변 정상잔액 계정은 평가 증가를 손실로 보고 계정 라인을 대변에 기록한다")
    void processFxValuationReversesSideForCreditNormalBalanceAccount() {
        LocalDate valuationDate = LocalDate.of(2026, 5, 31);
        FxValuationBalance balance = balance("221000", "EUR", "100.00", "110.00");

        when(fxExchangeRateLookupPort.findRate("EUR", "USD", valuationDate))
                .thenReturn(Optional.of(new BigDecimal("1.20000000")));
        when(masterDataQueryPort.findAccountSubjectAt("221000", valuationDate))
                .thenReturn(Optional.of(new AccountSubjectRef("221000", "EUR Borrowing", false, false, "CREDIT")));
        when(closingJournalEntryPort.createDraftAdjustment(any(ClosingJournalEntryCommand.class)))
                .thenReturn(new ClosingJournalEntryResult(502L, "FXV2026053177DEF"));

        service.processFxValuationForAccount(balance, valuationDate, 77L);

        ArgumentCaptor<ClosingJournalEntryCommand> captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        ClosingJournalEntryCommand command = captor.getValue();

        assertThat(command.lines().get(0).side()).isEqualTo(ClosingJournalSide.CREDIT);
        assertThat(command.lines().get(0).accountCode()).isEqualTo("221000");
        assertThat(command.lines().get(1).side()).isEqualTo(ClosingJournalSide.DEBIT);
        assertThat(command.lines().get(1).accountCode()).isEqualTo("920100");
    }

    @Test
    @DisplayName("장부 기준통화 잔액이 없으면 가상 장부환율을 만들지 않고 평가를 건너뛴다")
    void processFxValuationSkipsWhenBaseEndingBalanceIsMissing() {
        LocalDate valuationDate = LocalDate.of(2026, 5, 31);
        FxValuationBalance balance = balance("113000", "EUR", "100.00", null);

        when(fxExchangeRateLookupPort.findRate("EUR", "USD", valuationDate))
                .thenReturn(Optional.of(new BigDecimal("1.20000000")));

        service.processFxValuationForAccount(balance, valuationDate, 77L);

        verify(closingJournalEntryPort, never()).createDraftAdjustment(any());
    }

    private static FxValuationBalance balance(String accountCode, String currencyCode, String foreignAmount,
                                              String baseEndingBalance) {
        return new FxValuationBalance(
                accountCode,
                currencyCode,
                new BigDecimal(foreignAmount),
                baseEndingBalance == null ? null : new BigDecimal(baseEndingBalance));
    }
}