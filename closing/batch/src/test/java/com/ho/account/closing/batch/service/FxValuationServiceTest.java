package com.ho.account.closing.batch.service;

import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.masterdata.core.domain.model.ExchangeRate;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ExchangeRateRepository;
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
    private ExchangeRateRepository exchangeRateRepository;
    @Mock
    private JournalUseCase journalUseCase;

    private ClosingAccountingProperties accountingProperties;
    private FxValuationService service;

    @BeforeEach
    void setUp() {
        accountingProperties = new ClosingAccountingProperties();
        accountingProperties.setFxValuationReportingCurrencyCode("USD");
        accountingProperties.setFxTranslationGainAccountCode("720100");
        accountingProperties.setFxTranslationLossAccountCode("920100");
        service = new FxValuationService(exchangeRateRepository, journalUseCase, accountingProperties);
    }

    @Test
    @DisplayName("FX 평가는 정책 보고통화와 장부 기준통화 잔액으로 차액 전표를 생성한다")
    void processFxValuationUsesPolicyCurrencyAndBaseEndingBalance() {
        LocalDate valuationDate = LocalDate.of(2026, 5, 31);
        GlAccountBalance balance = balance("113000", "EUR", "100.00", "110.00");

        when(exchangeRateRepository.findExchangeRate("EUR", "USD", valuationDate))
                .thenReturn(Optional.of(exchangeRate("EUR", "USD", "1.20000000", valuationDate)));
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(501L);
            return entry;
        });

        service.processFxValuationForAccount(balance, valuationDate, 77L);

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalUseCase).createJournalEntry(captor.capture());
        JournalEntry entry = captor.getValue();

        assertThat(entry.getCurrencyCode()).isEqualTo("USD");
        assertThat(entry.getLineageSourceId()).isEqualTo("77");
        assertThat(entry.getDetails()).hasSize(2);
        assertThat(entry.getDetails().get(0).getSide()).isEqualTo(JournalSide.DEBIT);
        assertThat(entry.getDetails().get(0).getAccountCode()).isEqualTo("113000");
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("10.00");
        assertThat(entry.getDetails().get(1).getSide()).isEqualTo(JournalSide.CREDIT);
        assertThat(entry.getDetails().get(1).getAccountCode()).isEqualTo("720100");
        assertThat(entry.getDetails().get(1).getAmount()).isEqualByComparingTo("10.00");
        verify(journalUseCase).approveJournalEntry(501L, "SYSTEM");
        verify(journalUseCase).postJournalEntry(501L, "SYSTEM");
    }

    @Test
    @DisplayName("장부 기준통화 잔액이 없으면 가상 장부환율을 만들지 않고 평가를 건너뛴다")
    void processFxValuationSkipsWhenBaseEndingBalanceIsMissing() {
        LocalDate valuationDate = LocalDate.of(2026, 5, 31);
        GlAccountBalance balance = balance("113000", "EUR", "100.00", null);

        when(exchangeRateRepository.findExchangeRate("EUR", "USD", valuationDate))
                .thenReturn(Optional.of(exchangeRate("EUR", "USD", "1.20000000", valuationDate)));

        service.processFxValuationForAccount(balance, valuationDate, 77L);

        verify(journalUseCase, never()).createJournalEntry(any());
    }

    private static GlAccountBalance balance(String accountCode, String currencyCode, String foreignAmount,
                                            String baseEndingBalance) {
        GlAccountBalance balance = new GlAccountBalance();
        balance.setAccountCode(accountCode);
        balance.setCurrencyCode(currencyCode);
        balance.setEndingBalance(new BigDecimal(foreignAmount));
        if (baseEndingBalance != null) {
            balance.setBaseEndingBalance(new BigDecimal(baseEndingBalance));
        }
        return balance;
    }

    private static ExchangeRate exchangeRate(String fromCurrency, String toCurrency, String rate, LocalDate date) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setFromCurrencyCode(fromCurrency);
        exchangeRate.setToCurrencyCode(toCurrency);
        exchangeRate.setRate(new BigDecimal(rate));
        exchangeRate.setEffectiveDate(date);
        return exchangeRate;
    }
}
