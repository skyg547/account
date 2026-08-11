package com.ho.account.reconciliation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconciliationRunTest {

    @Test
    @DisplayName("startRun static 팩토리 메서드는 RUNNING 상태의 ReconciliationRun 객체를 생성한다")
    void startRun() {
        ReconciliationUnit unit = new ReconciliationUnit();
        LocalDate date = LocalDate.of(2026, 8, 12);

        ReconciliationRun run = ReconciliationRun.startRun(unit, date, "OPERATOR");

        assertThat(run.getStatus()).isEqualTo(ReconciliationRun.ReconciliationRunStatus.RUNNING);
        assertThat(run.getReconciliationUnit()).isSameAs(unit);
        assertThat(run.getReconciliationDate()).isEqualTo(date);
        assertThat(run.getRunBy()).isEqualTo("OPERATOR");
        assertThat(run.getRunStartTime()).isNotNull();
    }

    @Test
    @DisplayName("completeRun 호출 시 대사 집계 수치 세팅 및 SUCCESS 상태로 변경된다")
    void completeRunSuccess() {
        ReconciliationUnit unit = new ReconciliationUnit();
        ReconciliationRun run = ReconciliationRun.startRun(unit, LocalDate.now(), "OPERATOR");

        run.completeRun(
                10L, new BigDecimal("1000.00"),
                10L, new BigDecimal("1000.00"),
                10L, new BigDecimal("1000.00"),
                0L, BigDecimal.ZERO
        );

        assertThat(run.getStatus()).isEqualTo(ReconciliationRun.ReconciliationRunStatus.SUCCESS);
        assertThat(run.getTotalItemsSource()).isEqualTo(10L);
        assertThat(run.getMatchedAmount()).isEqualByComparingTo("1000.00");
        assertThat(run.getRunEndTime()).isNotNull();
    }

    @Test
    @DisplayName("completeRun 호출 시 RUNNING 상태가 아니면 IllegalStateException이 발생한다")
    void completeRunThrowsExceptionWhenNotRunning() {
        ReconciliationUnit unit = new ReconciliationUnit();
        ReconciliationRun run = ReconciliationRun.startRun(unit, LocalDate.now(), "OPERATOR");
        run.failRun();

        assertThatThrownBy(() -> run.completeRun(
                10L, new BigDecimal("1000.00"),
                10L, new BigDecimal("1000.00"),
                10L, new BigDecimal("1000.00"),
                0L, BigDecimal.ZERO
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot complete a reconciliation run that is not in RUNNING state");
    }

    @Test
    @DisplayName("failRun 호출 시 FAILED 상태 및 종료 시간이 세팅된다")
    void failRunSuccess() {
        ReconciliationUnit unit = new ReconciliationUnit();
        ReconciliationRun run = ReconciliationRun.startRun(unit, LocalDate.now(), "OPERATOR");

        run.failRun();

        assertThat(run.getStatus()).isEqualTo(ReconciliationRun.ReconciliationRunStatus.FAILED);
        assertThat(run.getRunEndTime()).isNotNull();
    }
}
