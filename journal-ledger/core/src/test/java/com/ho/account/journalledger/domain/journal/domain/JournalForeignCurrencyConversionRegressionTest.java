package com.ho.account.journalledger.domain.journal.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Compatibility regression: this class deliberately uses only JournalEntry/JournalDetail APIs that
 * existed at the audited revision so the same source can demonstrate RED there and GREEN here.
 */
class JournalForeignCurrencyConversionRegressionTest {

    @Test
    @DisplayName("USD 100 at 1300 rejects a one-to-one base amount")
    void rejectsForeignAmountThatWasNotConverted() {
        JournalEntry entry = twoLineEntry("USD", "1300", "100.00", "100.00");

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기준통화");
    }

    @Test
    @DisplayName("USD 100 at 1300 accepts KRW 130000 on both accounting sides")
    void acceptsSemanticallyConvertedForeignAmount() {
        JournalEntry entry = twoLineEntry("USD", "1300", "100.00", "130000.00");

        assertThatCode(entry::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("foreign currency requires explicit transaction-to-base rate")
    void rejectsForeignEntryWithoutExchangeRate() {
        JournalEntry entry = twoLineEntry("USD", null, "100.00", "130000.00");

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("환율");
    }

    @Test
    @DisplayName("KRW base-currency entry cannot carry a non-one exchange rate")
    void rejectsNonOneRateForBaseCurrency() {
        JournalEntry entry = twoLineEntry("KRW", "1.01", "100.00", "100.00");

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1");
    }

    @Test
    @DisplayName("largest-remainder allocation preserves exact base debit and credit equality")
    void acceptsControlledLargestRemainderAllocation() {
        JournalEntry entry = entry("USD", "1.33333333");
        entry.addDetail(detail(JournalSide.DEBIT, "11000", ".01", ".01"));
        entry.addDetail(detail(JournalSide.DEBIT, "12000", ".02", ".03"));
        entry.addDetail(detail(JournalSide.CREDIT, "21000", ".03", ".04"));

        assertThatCode(entry::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("independently balanced but uncontrolled residual allocation is rejected")
    void rejectsBalancedButUncontrolledResidualAllocation() {
        JournalEntry entry = entry("USD", "1.33333333");
        entry.addDetail(detail(JournalSide.DEBIT, "11000", ".01", ".02"));
        entry.addDetail(detail(JournalSide.DEBIT, "12000", ".02", ".02"));
        entry.addDetail(detail(JournalSide.CREDIT, "21000", ".03", ".04"));

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기준통화");
    }

    @Test
    @DisplayName("equal remainders accept the required adjusted-line count regardless of encounter order")
    void acceptsEitherLineWithinAnEqualRemainderTie() {
        JournalEntry firstAdjusted = tiedRemainderEntry(".02", ".01");
        JournalEntry secondAdjusted = tiedRemainderEntry(".01", ".02");

        assertThatCode(firstAdjusted::validateInvariants).doesNotThrowAnyException();
        assertThatCode(secondAdjusted::validateInvariants).doesNotThrowAnyException();
    }

    private static JournalEntry tiedRemainderEntry(String firstBase, String secondBase) {
        JournalEntry entry = entry("USD", "1.5");
        entry.addDetail(detail(JournalSide.DEBIT, "11000", ".01", firstBase));
        entry.addDetail(detail(JournalSide.DEBIT, "12000", ".01", secondBase));
        entry.addDetail(detail(JournalSide.CREDIT, "21000", ".02", ".03"));
        return entry;
    }

    private static JournalEntry twoLineEntry(
            String currencyCode, String exchangeRate, String amount, String baseAmount) {
        JournalEntry entry = entry(currencyCode, exchangeRate);
        entry.addDetail(detail(JournalSide.DEBIT, "11000", amount, baseAmount));
        entry.addDetail(detail(JournalSide.CREDIT, "21000", amount, baseAmount));
        return entry;
    }

    private static JournalEntry entry(String currencyCode, String exchangeRate) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.of(2026, 9, 25));
        entry.setCreatedBy("fx-regression-maker");
        entry.setCurrencyCode(currencyCode);
        if (exchangeRate != null) {
            entry.setExchangeRate(new BigDecimal(exchangeRate));
        }
        return entry;
    }

    private static JournalDetail detail(
            JournalSide side, String accountCode, String amount, String baseAmount) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(baseAmount));
        return detail;
    }
}
