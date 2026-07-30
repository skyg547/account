package com.ho.account.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BudgetTransferExecutionTest {

    @Test
    void transferPreservesRequestLineageAndAllowsOnlyRequestedToApproved() {
        BudgetTransfer transfer =
                BudgetTransfer.request(" request-1 ", 10L, 20L, new BigDecimal("12.50"), " maker ");

        assertThat(transfer.requestKey()).isEqualTo("request-1");
        assertThat(transfer.sourcePlanId()).isEqualTo(10L);
        assertThat(transfer.targetPlanId()).isEqualTo(20L);
        assertThat(transfer.amount()).isEqualByComparingTo("12.50");
        assertThat(transfer.status()).isEqualTo(BudgetTransferStatus.REQUESTED);
        assertThat(transfer.requestedBy()).isEqualTo("maker");
        assertThat(transfer.hasSameRequest(10L, 20L, new BigDecimal("12.500"), "maker")).isTrue();
        assertThat(transfer.hasSameRequest(20L, 10L, new BigDecimal("12.50"), "maker")).isFalse();

        transfer.approve(" checker ");

        assertThat(transfer.status()).isEqualTo(BudgetTransferStatus.APPROVED);
        assertThat(transfer.approvedBy()).isEqualTo("checker");
        assertThatIllegalStateException()
                .isThrownBy(() -> transfer.approve("another"))
                .withMessageContaining("요청 상태");
    }

    @Test
    void transferRejectsSelfTransferInvalidPrecisionAndInconsistentSnapshots() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetTransfer.request(
                        "request-1", 10L, 10L, BigDecimal.ONE, "maker"))
                .withMessageContaining("달라야");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetTransfer.request(
                        "request-1", 10L, 20L, new BigDecimal("1.001"), "maker"))
                .withMessageContaining("소수점 둘째 자리");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetTransfer.restore(
                        1L,
                        "request-1",
                        10L,
                        20L,
                        BigDecimal.ONE,
                        BudgetTransferStatus.APPROVED,
                        "maker",
                        null))
                .withMessageContaining("승인자가 필요");
    }

    @Test
    void executionPreservesSourceTripleAndCancellationIsIdempotent() {
        LocalDate date = LocalDate.of(2026, 1, 31);
        BudgetExecution execution = BudgetExecution.execute(
                10L, " AP ", " invoice-1 ", " line-3 ", date, new BigDecimal("8.25"), " executor ");

        assertThat(execution.budgetPlanId()).isEqualTo(10L);
        assertThat(execution.sourceType()).isEqualTo("AP");
        assertThat(execution.sourceId()).isEqualTo("invoice-1");
        assertThat(execution.sourceLineId()).isEqualTo("line-3");
        assertThat(execution.executionDate()).isEqualTo(date);
        assertThat(execution.amount()).isEqualByComparingTo("8.25");
        assertThat(execution.status()).isEqualTo(BudgetExecutionStatus.EXECUTED);
        assertThat(execution.hasSameExecution(10L, date, new BigDecimal("8.250"), "executor")).isTrue();
        assertThat(execution.hasSameExecution(10L, date.plusDays(1), new BigDecimal("8.25"), "executor"))
                .isFalse();

        execution.cancel(" canceller ");
        execution.cancel("retrying-canceller");

        assertThat(execution.status()).isEqualTo(BudgetExecutionStatus.CANCELLED);
        assertThat(execution.cancelledBy()).isEqualTo("canceller");
    }

    @Test
    void executionRejectsInvalidLineageAmountAndInconsistentSnapshots() {
        LocalDate date = LocalDate.of(2026, 1, 31);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetExecution.execute(
                        1L, "AP", "invoice-1", " ", date, BigDecimal.ONE, "executor"))
                .withMessageContaining("sourceLineId");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetExecution.execute(
                        1L, "AP", "invoice-1", "line-1", date, BigDecimal.ZERO, "executor"))
                .withMessageContaining("0보다 커야");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BudgetExecution.restore(
                        1L,
                        1L,
                        "AP",
                        "invoice-1",
                        "line-1",
                        date,
                        BigDecimal.ONE,
                        BudgetExecutionStatus.CANCELLED,
                        "executor",
                        null))
                .withMessageContaining("취소자가 필요");
    }
}
