package com.ho.account.closing.batch.adapter.out;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.ho.account.closing.batch.adapter.out.EclJournalFixture.DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

class GlAllowanceBalanceLookupAdapterTest {
    private EclJournalFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new EclJournalFixture();
    }

    @Test
    void postedCreditMinusDebitKeepsActualTransactionAndBaseAmounts() {
        fixture.ordinary("POSTED", DATE.minusMonths(1), "USD", "129100", "CREDIT", "100", "130000");
        fixture.ordinary("POSTED", DATE, "USD", "129100", "DEBIT", "20", "26000");
        fixture.ordinary("DRAFT", DATE, "USD", "129100", "CREDIT", "900", "1170000");
        fixture.ordinary("APPROVED", DATE, "USD", "129100", "CREDIT", "700", "910000");
        fixture.ordinary("POSTED", DATE.plusDays(1), "USD", "129100", "CREDIT", "600", "780000");
        fixture.ordinary("POSTED", DATE, "USD", "139100", "CREDIT", "500", "650000");
        fixture.jdbc.update("INSERT INTO gl_balances VALUES ('129100', 'USD', ?, '2026-05', -999999)", DATE);

        assertBalance("USD", "KRW", "80", "104000");
        verifyNoInteractions(fixture.eligibility);
    }

    @Test
    void sameAccountCurrenciesAndOrdinaryFunctionalAmountsStaySeparate() {
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", "80", "104000");
        fixture.ordinary("POSTED", DATE, "EUR", "129100", "CREDIT", "50", "75000");
        fixture.ordinary("POSTED", DATE, "KRW", "129100", "CREDIT", "999", "999");
        fixture.fx("POSTED", DATE, "KRW", "780|129100|USD", "129100", "CREDIT", "8000");

        assertBalance("USD", "KRW", "80", "112000");
        assertBalance("EUR", "KRW", "50", "75000");
        assertBalance("KRW", "KRW", "999", "999");
    }

    @Test
    void emptyBalanceReturnsExplicitUnitsAndZeroForBothAmounts() {
        assertBalance("USD", "EUR", "0", "0");
    }

    @Test
    void ordinaryPostedReversalOffsetsBothUnitsAndPreservesDebitBalance() {
        long original = fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", "80", "104000");
        fixture.reverse(original, "POSTED", DATE);
        fixture.ordinary("POSTED", DATE, "USD", "129100", "DEBIT", "20", "26000");

        assertBalance("USD", "KRW", "-20", "-26000");
    }

    @ParameterizedTest
    @CsvSource({"DRAFT, 0, 112000", "APPROVED, 0, 112000", "POSTED, 1, 112000", "POSTED, 0, 104000"})
    void fxReversalUsesItsOwnPostedStatusAndDate(String status, int daysAfterCutoff, String expectedBase) {
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", "80", "104000");
        long fx = fixture.fx("POSTED", DATE, "KRW", "780|129100|USD", "129100", "CREDIT", "8000");
        fixture.reverse(fx, status, DATE.plusDays(daysAfterCutoff));

        assertBalance("USD", "KRW", "80", expectedBase);
    }

    @Test
    void fxReversalOfReversalAndFutureRootRetainSourceCurrencyAttribution() {
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", "80", "104000");
        long fx = fixture.fx("POSTED", DATE.plusDays(1), "KRW", "780|129100|USD", "129100", "CREDIT", "8000");
        long reversal = fixture.reverse(fx, "POSTED", DATE);
        assertBalance("USD", "KRW", "80", "96000");
        fixture.reverse(reversal, "POSTED", DATE);
        assertBalance("USD", "KRW", "80", "104000");
    }

    @Test
    void adjustmentOnlyResidualIsVisibleForCoreReconciliationGuard() {
        fixture.fx("POSTED", DATE, "KRW", "780|129100|USD", "129100", "CREDIT", "8000");
        assertBalance("USD", "KRW", "0", "8000");
    }

    @ParameterizedTest
    @CsvSource({"bad-lineage, KRW, 129100", "780|129100|USD, EUR, 129100", "780|129100|USD, KRW, 139100"})
    void malformedOrUnattributablePostedFxHistoryFailsClosed(String lineage, String currency, String account) {
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", "80", "104000");
        fixture.fx("POSTED", DATE, currency, lineage, account, "CREDIT", "8000");
        assertThatThrownBy(() -> fixture.balances.findCreditBalance("129100", "USD", "KRW", DATE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("lineage");
    }

    @Test
    void malformedInactiveFxHistoryDoesNotChangeCurrentPostedBalance() {
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", "80", "104000");
        fixture.fx("DRAFT", DATE, "KRW", "bad-lineage", "129100", "CREDIT", "8000");
        fixture.fx("POSTED", DATE.plusDays(1), "KRW", "bad-lineage", "129100", "CREDIT", "8000");
        assertBalance("USD", "KRW", "80", "104000");
    }

    private void assertBalance(String transactionCurrency, String functionalCurrency, String transaction, String base) {
        var balance = fixture.balances.findCreditBalance("129100", transactionCurrency, functionalCurrency, DATE);
        assertThat(balance.transactionCurrencyCode()).isEqualTo(transactionCurrency);
        assertThat(balance.functionalCurrencyCode()).isEqualTo(functionalCurrency);
        assertThat(balance.creditTransactionAmount()).isEqualByComparingTo(transaction);
        assertThat(balance.creditBaseAmount()).isEqualByComparingTo(base);
    }
}
