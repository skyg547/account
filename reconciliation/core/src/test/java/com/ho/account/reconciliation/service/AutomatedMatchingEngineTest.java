package com.ho.account.reconciliation.service;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalSide;
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
    void matchPreservesJournalDetailInputOrderWithinAmountTolerance() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary firstDetail = journalDetail("100.50", LocalDate.of(2026, 5, 12));
        JournalDetailSummary secondDetail = journalDetail("100.00", LocalDate.of(2026, 5, 12));

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(
                List.of(statement),
                List.of(firstDetail, secondDetail),
                AutomatedMatchingEngine.MatchOptions.of(new BigDecimal("1.00"), 0));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getJournalDetail()).isSameAs(firstDetail);
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.TOLERANCE_DATE_AMOUNT_MATCH);
    }

    @Test
    void matchSucceedsWithSlipNoInDescription() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        statement.setDescription("Payment for slip-001");

        JournalDetailSummary detail = journalDetail("100.00", LocalDate.of(2026, 5, 20));
        detail.setSlipNo("SLIP-001");

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(
                List.of(statement),
                List.of(detail),
                AutomatedMatchingEngine.MatchOptions.complex(BigDecimal.ZERO, 0, false, true, false));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.SLIP_NO_MATCH);
    }

    @Test
    void matchSucceedsWithDescriptionSimilarity() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        statement.setDescription("Invoice 12345 from ABC Corp");

        JournalDetailSummary detail = journalDetail("100.00", LocalDate.of(2026, 5, 20));
        detail.setDetailDescription("Invoice 12345");

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(
                List.of(statement),
                List.of(detail),
                AutomatedMatchingEngine.MatchOptions.complex(BigDecimal.ZERO, 0, true, false, false));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.COMPLEX_DESCRIPTION_MATCH);
    }

    @Test
    void matchSucceedsWithAccountNoIgnoringSeparators() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        statement.setAccountNo("123-456-789");

        JournalDetailSummary detail = journalDetail("100.00", LocalDate.of(2026, 5, 20));
        detail.setAccountNo("123456789");

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(
                List.of(statement),
                List.of(detail),
                AutomatedMatchingEngine.MatchOptions.complex(BigDecimal.ZERO, 0, false, false, true));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.ACCOUNT_NO_MATCH);
    }

    @Test
    void exactMatchKeepsLegacyReasonWhenComplexOptionsAreDisabled() {
        BankStatement statement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        statement.setDescription("Payment for SLIP-001");

        JournalDetailSummary detail = journalDetail("100.00", LocalDate.of(2026, 5, 12));
        detail.setSlipNo("SLIP-001");

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(List.of(statement), List.of(detail));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.EXACT_DATE_AMOUNT_MATCH);
    }

    @Test
    void matchConsumesJournalDetailOnce() {
        BankStatement firstStatement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        BankStatement secondStatement = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary detail = journalDetail("100.00", LocalDate.of(2026, 5, 12));

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(
                List.of(firstStatement, secondStatement),
                List.of(detail));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getJournalDetail()).isSameAs(detail);
        assertThat(results.get(1).isMatch()).isFalse();
        assertThat(results.get(1).getJournalDetail()).isNull();
        assertThat(results.get(1).getMatchReason()).isEqualTo(AutomatedMatchingEngine.NO_MATCH_FOUND);
    }

    @Test
    void matchUsesSignedDirectionForBankAndJournalAmounts() {
        BankStatement withdrawal = bankStatement("0.00", "100.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary creditDetail = journalDetail("100.00", LocalDate.of(2026, 5, 12));
        creditDetail.setSide(JournalSide.CREDIT);

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(List.of(withdrawal), List.of(creditDetail));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isTrue();
        assertThat(results.get(0).getJournalDetail()).isSameAs(creditDetail);
    }

    @Test
    void matchRejectsOppositeSignedDirectionEvenWhenAbsoluteAmountMatches() {
        BankStatement deposit = bankStatement("100.00", "0.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary creditDetail = journalDetail("100.00", LocalDate.of(2026, 5, 12));
        creditDetail.setSide(JournalSide.CREDIT);

        List<AutomatedMatchingEngine.MatchResult> results = matchingEngine.match(List.of(deposit), List.of(creditDetail));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatch()).isFalse();
        assertThat(results.get(0).getJournalDetail()).isNull();
        assertThat(results.get(0).getMatchReason()).isEqualTo(AutomatedMatchingEngine.NO_MATCH_FOUND);
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

    @Test
    void matchSubsetSumSucceedsForNMStatementsAndJournalDetails() {
        BankStatement stmt1 = bankStatement("30000.00", "0.00", LocalDate.of(2026, 5, 12));
        BankStatement stmt2 = bankStatement("70000.00", "0.00", LocalDate.of(2026, 5, 12));

        JournalDetailSummary detail1 = journalDetail("40000.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary detail2 = journalDetail("40000.00", LocalDate.of(2026, 5, 12));
        JournalDetailSummary detail3 = journalDetail("20000.00", LocalDate.of(2026, 5, 12));

        List<AutomatedMatchingEngine.SubsetMatchResult> results = matchingEngine.matchSubsetSum(
                List.of(stmt1, stmt2),
                List.of(detail1, detail2, detail3),
                3);

        assertThat(results).hasSize(1);
        AutomatedMatchingEngine.SubsetMatchResult match = results.get(0);
        assertThat(match.isMatch()).isTrue();
        assertThat(match.getStatements()).containsExactlyInAnyOrder(stmt1, stmt2);
        assertThat(match.getJournalDetails()).containsExactlyInAnyOrder(detail1, detail2, detail3);
        assertThat(match.getTotalAmount()).isEqualByComparingTo("100000.00");
        assertThat(match.getMatchReason()).isEqualTo(AutomatedMatchingEngine.SUBSET_SUM_MATCH);
    }

    @Test
    void matchRejectsNullArgumentsFailFast() {
        assertThatThrownBy(() -> matchingEngine.match(null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("statements must not be null");

        assertThatThrownBy(() -> matchingEngine.match(List.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("details must not be null");

        assertThatThrownBy(() -> matchingEngine.match(List.of(), List.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("options must not be null");
    }

    @Test
    void matchSubsetSumRejectsNullArgumentsFailFast() {
        assertThatThrownBy(() -> matchingEngine.matchSubsetSum(null, List.of(), 3))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("statements must not be null");

        assertThatThrownBy(() -> matchingEngine.matchSubsetSum(List.of(), null, 3))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("details must not be null");
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
