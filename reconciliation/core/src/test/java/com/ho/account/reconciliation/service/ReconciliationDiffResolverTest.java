package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReconciliationDiffResolver 단위 테스트")
class ReconciliationDiffResolverTest {

    private final ReconciliationDiffResolver resolver = new ReconciliationDiffResolver();

    @Test
    @DisplayName("대사 차이 항목에 사유 코드를 부여하고 RESOLVED 상태로 바르게 해소되는지 검증한다")
    void resolveDifference_Success() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING);
        difference.setDifferenceAmount(new BigDecimal("5000.00"));
        difference.setDescription("Bank transaction missing in GL");

        DifferenceReasonCode reasonCode = new DifferenceReasonCode();
        reasonCode.setCode("BANK_FEE_OMISSION");
        reasonCode.setName("은행 수수료 누락");

        resolver.resolveDifference(difference, reasonCode, "수동 전표 생성으로 차이 해소", "AUDITOR");

        assertThat(difference.getStatus()).isEqualTo(ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED);
        assertThat(difference.getReasonCode()).isEqualTo(reasonCode);
        assertThat(difference.getResolvedBy()).isEqualTo("AUDITOR");
        assertThat(difference.getResolvedAt()).isNotNull();
        assertThat(difference.getDescription()).contains("Bank transaction missing in GL");
    }

    @Test
    @DisplayName("이미 RESOLVED 된 차이 항목에 대해 재해소 시도 시 예외가 발생한다")
    void resolveDifference_AlreadyResolved() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED);

        DifferenceReasonCode reasonCode = new DifferenceReasonCode();

        assertThatThrownBy(() -> resolver.resolveDifference(difference, reasonCode, "재처리", "AUDITOR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Difference is already resolved");
    }

    @Test
    @DisplayName("차이 금액에 따른 조정 전표 필요 금액이 바르게 산출되는지 검증한다")
    void calculateAdjustmentAmount() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setDifferenceAmount(new BigDecimal("-15000.00"));

        BigDecimal adjustmentAmount = resolver.calculateAdjustmentAmount(difference);

        assertThat(adjustmentAmount).isEqualByComparingTo("15000.00");
    }
}
