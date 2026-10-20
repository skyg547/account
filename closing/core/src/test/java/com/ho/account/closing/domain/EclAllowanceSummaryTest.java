package com.ho.account.closing.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EclAllowanceSummaryTest {

    @Test
    void rejectsUnreconciledStagesAtDirectPortBoundary() {
        assertThatThrownBy(() -> summary("100", "1", "2", "3", "1000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stage allowance total");
    }

    @Test
    void reconcilesExactHighPrecisionValuesWithoutPostingRounding() {
        var summary = summary("100.0001", "33.3333", "33.3333", "33.3335", "1000");
        assertThat(summary.stage1AllowanceAmount().add(summary.stage2AllowanceAmount())
                .add(summary.stage3AllowanceAmount())).isEqualByComparingTo(summary.targetAllowanceAmount());
    }

    @Test
    void zeroExposureRequiresZeroTargetAndStages() {
        assertThat(summary("0", "0", "0", "0", "0").targetAllowanceAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThatThrownBy(() -> summary("1", "1", "0", "0", "0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero source exposure");
    }

    private static EclAllowanceSummary summary(String target, String stage1, String stage2,
                                                String stage3, String exposure) {
        return new EclAllowanceSummary(LocalDate.of(2026, 5, 31), "RUN", "MODEL", "ENTITY",
                "KRW", "12000", "129100", "550100", "480100", new BigDecimal(target),
                new BigDecimal(exposure), new BigDecimal(stage1), new BigDecimal(stage2),
                new BigDecimal(stage3));
    }
}
