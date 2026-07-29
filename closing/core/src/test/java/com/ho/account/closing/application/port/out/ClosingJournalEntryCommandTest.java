package com.ho.account.closing.application.port.out;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClosingJournalEntryCommandTest {

    @Test
    void commandNormalizesCurrencyAndCopiesLines() {
        ClosingJournalEntryCommand command = command(
                "krw",
                line(ClosingJournalSide.DEBIT, "100"),
                line(ClosingJournalSide.CREDIT, "100"));

        assertThat(command.currencyCode()).isEqualTo("KRW");
        assertThat(command.lines()).hasSize(2);
    }

    @Test
    void commandRejectsTransactionAmountImbalance() {
        assertThatThrownBy(() -> command(
                "KRW",
                line(ClosingJournalSide.DEBIT, "100"),
                line(ClosingJournalSide.CREDIT, "90")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount is not balanced");
    }

    @Test
    void commandRejectsBaseAmountImbalance() {
        ClosingJournalLineCommand debit = new ClosingJournalLineCommand(
                ClosingJournalSide.DEBIT, "11000", new BigDecimal("100"), new BigDecimal("100"), "");
        ClosingJournalLineCommand credit = new ClosingJournalLineCommand(
                ClosingJournalSide.CREDIT, "11000", new BigDecimal("100"), new BigDecimal("90"), "");

        assertThatThrownBy(() -> command("USD", debit, credit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseAmount is not balanced");
    }

    @Test
    void commandRejectsNonAlphabeticCurrency() {
        assertThatThrownBy(() -> command(
                "12$",
                line(ClosingJournalSide.DEBIT, "100"),
                line(ClosingJournalSide.CREDIT, "100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3-letter currency code");
    }

    private static ClosingJournalEntryCommand command(
            String currencyCode,
            ClosingJournalLineCommand... lines) {
        LocalDate date = LocalDate.of(2026, 5, 31);
        return new ClosingJournalEntryCommand(
                date,
                date,
                "Closing",
                "CLOSING_ADJUSTMENT",
                "SYSTEM",
                "SYSTEM",
                "TEST",
                "1",
                currencyCode,
                "ECL20260531ABCDEF123",
                List.of(lines));
    }

    private static ClosingJournalLineCommand line(ClosingJournalSide side, String amount) {
        return new ClosingJournalLineCommand(
                side,
                "11000",
                new BigDecimal(amount),
                new BigDecimal(amount),
                "");
    }
}
