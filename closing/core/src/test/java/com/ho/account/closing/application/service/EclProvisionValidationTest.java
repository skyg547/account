package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.AllowanceBalance;
import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.closing.domain.EclAllowanceSummary;
import com.ho.account.closing.domain.ProvisionBatch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EclProvisionValidationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 5, 31);
    private final AllowanceBalanceLookupPort balances = mock(AllowanceBalanceLookupPort.class);
    private final ClosingJournalEntryPort journals = mock(ClosingJournalEntryPort.class);
    private final EclAllowanceResultPort summaries = mock(EclAllowanceResultPort.class);
    private final FxExchangeRateLookupPort rates = mock(FxExchangeRateLookupPort.class);
    private final ClosingAccountingProperties properties = new ClosingAccountingProperties();
    private EclProvisionService service;

    @BeforeEach
    void setUp() {
        var rule = new ClosingAccountingProperties.EclAccountMapping();
        rule.setDebitAccountCode("550100");
        rule.setCreditAccountCode("129100");
        properties.getProvisionRules().put(ProvisionBatch.ProvisionType.ECL, rule);
        service = new EclProvisionService(balances, journals, properties, summaries, rates);
        when(summaries.loadSummaries(DATE)).thenReturn(List.of(summary("USD", "100", "480100")));
        when(balances.findCreditBalance("129100", "USD", "KRW", DATE))
                .thenReturn(balance("USD", "KRW", "80", "104000"));
        when(rates.findRate("USD", "KRW", DATE)).thenReturn(Optional.of(new BigDecimal("1300")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.000000001", "1300.123456789", "100000000000"})
    void invalidOrUnrepresentableClosingRateFailsBeforeRemoteWrite(String invalidRate) {
        when(rates.findRate("USD", "KRW", DATE)).thenReturn(Optional.of(new BigDecimal(invalidRate)));
        assertThatThrownBy(() -> service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exchangeRate");
        verifyNoInteractions(journals);
    }

    @ParameterizedTest
    @CsvSource({"EUR, KRW", "USD, EUR"})
    void balanceCurrencyLabelsMustMatchRequestedUnits(String transaction, String functional) {
        when(balances.findCreditBalance("129100", "USD", "KRW", DATE))
                .thenReturn(balance(transaction, functional, "80", "104000"));
        assertThatThrownBy(() -> service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("currency units");
        verifyNoInteractions(journals, rates);
    }

    @Test
    void missingTypedBalanceIsNeverTreatedAsZero() {
        when(balances.findCreditBalance("129100", "USD", "KRW", DATE)).thenReturn(null);
        assertThatThrownBy(() -> service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(journals, rates);
    }

    @ParameterizedTest
    @CsvSource({"100000000000000000, 1", "100000000000000, 1300"})
    void transactionOrConvertedBaseOverflowFailsBeforeRemoteWrite(String target, String rate) {
        when(summaries.loadSummaries(DATE)).thenReturn(List.of(summary("USD", target, "480100")));
        when(balances.findCreditBalance("129100", "USD", "KRW", DATE))
                .thenReturn(balance("USD", "KRW", "0", "0"));
        when(rates.findRate("USD", "KRW", DATE)).thenReturn(Optional.of(new BigDecimal(rate)));
        assertThatThrownBy(() -> service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("precision");
        verifyNoInteractions(journals);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void laterStaleBalanceOrMissingReleaseMappingPreventsEarlierValidDraft(boolean staleBalance) {
        when(summaries.loadSummaries(DATE)).thenReturn(List.of(
                summary("EUR", "100", "480100"), summary("USD", "60", null)));
        when(balances.findCreditBalance("129100", "EUR", "KRW", DATE))
                .thenReturn(balance("EUR", "KRW", "0", "0"));
        when(rates.findRate("EUR", "KRW", DATE)).thenReturn(Optional.of(new BigDecimal("1500")));
        if (staleBalance) {
            when(rates.findRate("USD", "KRW", DATE)).thenReturn(Optional.of(new BigDecimal("1400")));
        }
        assertThatThrownBy(() -> service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(staleBalance ? "FX valuation" : "reversalIncomeAccountCode");
        verifyNoInteractions(journals);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "EURO", "12$"})
    void invalidFunctionalCurrencyFailsBeforeReadingBalance(String functional) {
        properties.setFxValuationReportingCurrencyCode(functional);
        assertThatThrownBy(() -> service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(balances, rates, journals);
    }

    @Test
    void functionalCurrencyCaseNormalizationIsIndependentOfDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            properties.setFxValuationReportingCurrencyCode("inr");
            assertThat(properties.requireFxValuationReportingCurrencyCode()).isEqualTo("INR");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void sameCurrencyAmountDisagreementRequiresSourceReconciliation() {
        when(summaries.loadSummaries(DATE)).thenReturn(List.of(summary("KRW", "100", "480100")));
        when(balances.findCreditBalance("129100", "KRW", "KRW", DATE))
                .thenReturn(balance("KRW", "KRW", "80", "79"));
        assertThatThrownBy(() -> service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("source reconciliation");
        verifyNoInteractions(rates, journals);
    }

    private static AllowanceBalance balance(String transaction, String functional, String amount, String base) {
        return new AllowanceBalance(transaction, functional, new BigDecimal(amount), new BigDecimal(base));
    }

    private static EclAllowanceSummary summary(String currency, String target, String reversal) {
        return new EclAllowanceSummary(DATE, "RUN-781", "MODEL-781", "SYNTHETIC", currency,
                "12000", "129100", "550100", reversal, new BigDecimal(target), new BigDecimal("10000"),
                new BigDecimal(target), BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
