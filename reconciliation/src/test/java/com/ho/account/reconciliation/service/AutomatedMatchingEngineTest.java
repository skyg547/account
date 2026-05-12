package com.ho.account.reconciliation.service;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.reconciliation.domain.BankStatement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutomatedMatchingEngineTest {

    private final AutomatedMatchingEngine matchingEngine = new AutomatedMatchingEngine();

    @Test
    void matchUsesContractJournalDetailSummary() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary detail = journalDetail("100.00", LocalDate.of(2026, 5, 12));

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(List.of(statement), List.of(detail));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getJournalDetail()).isSameAs(detail);
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.EXACT_DATE_AMOUNT_MATCH);
    }

    @Test
    void matchFailsWhenAccountingDateDiffers() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary detail = journalDetail("100.00", LocalDate.of(2026, 5, 11));

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(List.of(statement), List.of(detail));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isFalse();
        assertThat(results.get(0).getJournalDetail()).isNull();
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.NO_MATCH_FOUND);
    }

    @Test
    void matchCanUseAmountAndDateTolerance() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary detail = journalDetail("100.50", LocalDate.of(2026, 5, 13));

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(
                List.of(statement),
                List.of(detail),
                AutomatedMatchingEngine.MatchOptions.of(new BigDecimal("1.00"), 1));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getJournalDetail()).isSameAs(detail);
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.TOLERANCE_DATE_AMOUNT_MATCH);
    }

    @Test
    void matchFailsWhenToleranceIsExceeded() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary detail = journalDetail("101.01", LocalDate.of(2026, 5, 14));

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(
                List.of(statement),
                List.of(detail),
                AutomatedMatchingEngine.MatchOptions.of(new BigDecimal("1.00"), 1));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isFalse();
        assertThat(results.get(0).getJournalDetail()).isNull();
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.NO_MATCH_FOUND);
    }

    @Test
    void matchOptionsRejectNegativeTolerance() {
        assertThatThrownBy(() -> AutomatedMatchingEngine.MatchOptions.of(new BigDecimal("-0.01"), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amountTolerance");

        assertThatThrownBy(() -> AutomatedMatchingEngine.MatchOptions.of(BigDecimal.ZERO, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateToleranceDays");
    }

    private BankStatement bankStatement(String depositAmount, String withdrawalAmount, LocalDate transactionDate) {
        BankStatement statement = new BankStatement();
        statement.setDepositAmount(new BigDecimal(depositAmount));
        statement.setWithdrawalAmount(new BigDecimal(withdrawalAmount));
        statement.setTransactionDate(transactionDate);
        return statement;
    }

    private JournalDetailSummary journalDetail(String amount, LocalDate accountingDate) {
        JournalDetailSummary detail = new JournalDetailSummary();
        detail.setBaseAmount(new BigDecimal(amount));
        detail.setAccountingDate(accountingDate);
        return detail;
    }
}
