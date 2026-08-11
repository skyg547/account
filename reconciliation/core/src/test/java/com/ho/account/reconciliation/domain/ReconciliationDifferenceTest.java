package com.ho.account.reconciliation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconciliationDifferenceTest {

    @Test
    @DisplayName("createDifference 팩토리 메서드는 PENDING 상태의 대사 차이를 생성한다")
    void createDifference() {
        ReconciliationRun run = new ReconciliationRun();
        DifferenceReasonCode reasonCode = new DifferenceReasonCode();

        ReconciliationDifference diff = ReconciliationDifference.createDifference(
                run,
                ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH,
                new BigDecimal("1000.00"),
                new BigDecimal("900.00"),
                new BigDecimal("100.00"),
                "Amount mismatch",
                "sourceRef",
                "targetRef",
                reasonCode,
                "ADMIN"
        );

        assertThat(diff.getStatus()).isEqualTo(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING);
        assertThat(diff.getReconciliationRun()).isSameAs(run);
        assertThat(diff.getDifferenceAmount()).isEqualByComparingTo("100.00");
        assertThat(diff.getAuditUser()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("assignOwner 호출 시 담당자 배정 및 상태가 ASSIGNED로 전환된다")
    void assignOwnerSuccess() {
        ReconciliationDifference diff = new ReconciliationDifference();
        LocalDateTime dueDate = LocalDateTime.now().plusDays(1);

        diff.assignOwner("userA", dueDate);

        assertThat(diff.getStatus()).isEqualTo(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED);
        assertThat(diff.getAssignedToUser()).isEqualTo("userA");
        assertThat(diff.getSlaDueDate()).isEqualTo(dueDate);
    }

    @Test
    @DisplayName("assignOwner 호출 시 담당자가 빈 값이면 예외가 발생한다")
    void assignOwnerThrowsExceptionOnEmptyUser() {
        ReconciliationDifference diff = new ReconciliationDifference();

        assertThatThrownBy(() -> diff.assignOwner("   ", LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assigned user cannot be null or empty");
    }

    @Test
    @DisplayName("resolve 호출 시 사유코드가 조정 대상(adjustable)인데 전표가 없으면 예외가 발생한다")
    void resolveThrowsExceptionWhenAdjustableWithoutJournalEntry() {
        ReconciliationDifference diff = new ReconciliationDifference();
        DifferenceReasonCode adjustableReason = new DifferenceReasonCode();
        adjustableReason.setAdjustable(true);

        assertThatThrownBy(() -> diff.resolve(
                adjustableReason,
                null,
                ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED,
                "userB"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Adjustable reason code requires an adjustment journal entry link");
    }

    @Test
    @DisplayName("resolve 호출 시 정상적으로 RESOLVED 상태로 변경된다")
    void resolveSuccess() {
        ReconciliationDifference diff = new ReconciliationDifference();
        DifferenceReasonCode nonAdjustableReason = new DifferenceReasonCode();
        nonAdjustableReason.setAdjustable(false);

        diff.resolve(
                nonAdjustableReason,
                null,
                ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED,
                "userB"
        );

        assertThat(diff.getStatus()).isEqualTo(ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED);
        assertThat(diff.getReasonCode()).isSameAs(nonAdjustableReason);
        assertThat(diff.getResolvedBy()).isEqualTo("userB");
        assertThat(diff.getResolvedAt()).isNotNull();
    }
}
