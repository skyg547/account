package com.ho.account.contracts.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalContractValidationTest {

    @Test
    void copiesJournalLinesSoCallerMutationCannotChangeTheCommand() {
        JournalLineCommand line = line("DEBIT", "101000");
        List<JournalLineCommand> mutableLines = new ArrayList<>(List.of(line));

        JournalEntryCommand command = entry(mutableLines);
        mutableLines.clear();

        assertThat(command.lines()).containsExactly(line);
        assertThatThrownBy(() -> command.lines().add(line))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void normalizesAndValidatesJournalSideAndAccountCode() {
        JournalLineCommand line = line(" debit ", " 101000 ");

        assertThat(line.drcrType()).isEqualTo("DEBIT");
        assertThat(line.accountCode()).isEqualTo("101000");

        assertThatThrownBy(() -> line("BALANCE", "101000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DEBIT or CREDIT");
        assertThatThrownBy(() -> new JournalLineCommand(
                "DEBIT", "101000", null, null, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void requiresAtLeastOneJournalLine() {
        assertThatThrownBy(() -> entry(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lines");
    }

    @Test
    void validatesAccountNormalBalanceDirection() {
        AccountSubjectRef creditAccount =
                new AccountSubjectRef("221000", "Borrowing", false, false, " credit ");

        assertThat(creditAccount.normalBalanceSide()).isEqualTo("CREDIT");
        assertThat(creditAccount.creditNormalBalance()).isTrue();
        assertThat(new AccountSubjectRef("101000", "Cash", false, false).debitNormalBalance())
                .isTrue();
        assertThatThrownBy(() ->
                new AccountSubjectRef("101000", "Cash", false, false, "UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DEBIT or CREDIT");
        assertThatThrownBy(() ->
                new AccountSubjectRef("101000", "Cash", false, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("normalBalanceSide");
    }

    private JournalLineCommand line(String side, String accountCode) {
        return new JournalLineCommand(
                side,
                accountCode,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                null,
                null,
                "contract test");
    }

    private JournalEntryCommand entry(List<JournalLineCommand> lines) {
        return new JournalEntryCommand(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 22),
                "Contract test",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "tester",
                "tester",
                "TEST",
                "TEST-1",
                lines);
    }
}