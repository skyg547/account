package com.ho.account.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BudgetPlanTest {

    @Test
    void approvedPlanTracksTransfersExecutionsAndReleasedExecutionExactly() {
        BudgetPlan plan = BudgetPlan.create(
                " PLAN-1 ", "202601", " D001 ", " A100 ", new BigDecimal("100.00"), " maker ");

        assertThat(plan.planCode()).isEqualTo("PLAN-1");
        assertThat(plan.departmentCode()).isEqualTo("D001");
        assertThat(plan.accountCode()).isEqualTo("A100");
        assertThat(plan.status()).isEqualTo(BudgetPlanStatus.DRAFT);
        assertThat(plan.availableAmount()).isEqualByComparingTo("100.00");

        plan.approve(" checker ");
        plan.transferIn(new BigDecimal("10"));
        plan.transferOut(new BigDecimal("25.50"));
        plan.execute(LocalDate.of(2026, 1, 31), new BigDecimal("30.25"));

        assertThat(plan.approvedBy()).isEqualTo("checker");
        assertThat(plan.transferInAmount()).isEqualByComparingTo("10.00");
        assertThat(plan.transferOutAmount()).isEqualByComparingTo("25.50");
        assertThat(plan.executedAmount()).isEqualByComparingTo("30.25");
        assertThat(plan.availableAmount()).isEqualByComparingTo("54.25");

        plan.cancelExecution(new BigDecimal("5.25"));

        assertThat(plan.executedAmount()).isEqualByComparingTo("25.00");
        assertThat(plan.availableAmount()).isEqualByComparingTo("59.50");
    }

    @Test
    void insufficientFundsAndOverCancellationLeaveBalancesUnchanged() {
        BudgetPlan plan = approvedPlan("50.00");
        plan.execute(LocalDate.of(2026, 1, 31), new BigDecimal("20.00"));

        assertThatIllegalStateException()
                .isThrownBy(() -> plan.transferOut(new BigDecimal("30.01")))
                .withMessageContaining("가용 예산이 부족");
        assertThat(plan.transferOutAmount()).isEqualByComparingTo("0.00");
        assertThat(plan.availableAmount()).isEqualByComparingTo("30.00");

        assertThatIllegalStateException()
                .isThrownBy(() -> plan.cancelExecution(new BigDecimal("20.01")))
                .withMessageContaining("현재 집행액을 초과");
        assertThat(plan.executedAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void draftAndClosedPlansRejectFinancialMutationsAndInvalidTransitions() {
        BudgetPlan draft = BudgetPlan.create(
                "PLAN-D", "202601", "D001", "A100", new BigDecimal("100.00"), "maker");

        assertThatIllegalStateException()
                .isThrownBy(() -> draft.execute(LocalDate.of(2026, 1, 31), BigDecimal.ONE))
                .withMessageContaining("승인된 예산");
        assertThatIllegalStateException()
                .isThrownBy(() -> draft.close("closer"))
                .withMessageContaining("expected=APPROVED");

        draft.approve("checker");
        assertThatIllegalStateException()
                .isThrownBy(() -> draft.approve("second-checker"))
                .withMessageContaining("expected=DRAFT");

        draft.close("closer");
        assertThat(draft.status()).isEqualTo(BudgetPlanStatus.CLOSED);
        assertThat(draft.closedBy()).isEqualTo("closer");
        assertThatIllegalStateException()
                .isThrownBy(() -> draft.transferIn(BigDecimal.ONE))
                .withMessageContaining("승인된 예산");
        assertThatIllegalStateException()
                .isThrownBy(() -> draft.cancelExecution(BigDecimal.ONE))
                .withMessageContaining("승인된 예산");
    }

    @Test
    void precisionPolicyRejectsRoundingOverflowZeroAndNegativeAmounts() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetPlan.create(
                        "PLAN-1", "202601", "D001", "A100", new BigDecimal("1.001"), "maker"))
                .withMessageContaining("소수점 둘째 자리");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetPlan.create(
                        "PLAN-1",
                        "202601",
                        "D001",
                        "A100",
                        new BigDecimal("100000000000000000.00"),
                        "maker"))
                .withMessageContaining("DECIMAL(19,2)");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetPlan.create(
                        "PLAN-1", "202601", "D001", "A100", BigDecimal.ZERO, "maker"))
                .withMessageContaining("0보다 커야");

        BudgetPlan plan = approvedPlan("100.00");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> plan.execute(
                        LocalDate.of(2026, 1, 31), new BigDecimal("-0.01")))
                .withMessageContaining("0보다 커야");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> plan.transferIn(new BigDecimal("0.001")))
                .withMessageContaining("소수점 둘째 자리");
    }

    @Test
    void derivedAvailableAmountMayExceedOneStorageColumnPrecision() {
        BudgetPlan plan = approvedPlan("99999999999999999.99");

        plan.transferIn(new BigDecimal("99999999999999999.99"));

        assertThat(plan.availableAmount()).isEqualByComparingTo("199999999999999999.98");
    }

    @Test
    void transferAndExecutionRequireTheExactPlanYearMonthBeforeMutation() {
        BudgetPlan source = approvedPlan("100.00");
        BudgetPlan nextMonth = BudgetPlan.restore(
                2L,
                "NEXT",
                "202602",
                "D002",
                "A100",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BudgetPlanStatus.APPROVED,
                "maker",
                "checker",
                null);

        assertThatIllegalStateException()
                .isThrownBy(() -> source.transferTo(nextMonth, new BigDecimal("10.00")))
                .withMessageContaining("같은 YYYYMM");
        assertThatIllegalStateException()
                .isThrownBy(() -> source.execute(
                        LocalDate.of(2026, 2, 1), new BigDecimal("10.00")))
                .withMessageContaining("YYYYMM과 일치");
        assertThat(source.transferOutAmount()).isEqualByComparingTo("0.00");
        assertThat(nextMonth.transferInAmount()).isEqualByComparingTo("0.00");
        assertThat(source.executedAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void restoredStateMustBeInternallyConsistentAndNonNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetPlan.restore(
                        1L,
                        "PLAN-1",
                        "202601",
                        "D001",
                        "A100",
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("10.01"),
                        BudgetPlanStatus.APPROVED,
                        "maker",
                        "checker",
                        null))
                .withMessageContaining("가용액은 음수");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetPlan.restore(
                        1L,
                        "PLAN-1",
                        "202601",
                        "D001",
                        "A100",
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BudgetPlanStatus.CLOSED,
                        "maker",
                        "checker",
                        null))
                .withMessageContaining("승인자와 마감자");
    }

    private static BudgetPlan approvedPlan(String allocatedAmount) {
        BudgetPlan plan = BudgetPlan.create(
                "PLAN-1", "202601", "D001", "A100", new BigDecimal(allocatedAmount), "maker");
        plan.approve("checker");
        return plan;
    }
}
