package com.ho.account.reconciliation.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconciliationAdjustmentPolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReconciliationAdjustmentPolicy policy = new ReconciliationAdjustmentPolicy(objectMapper);

    @Test
    @DisplayName("정상적인 criteriaJson 설정으로부터 차변/대변 계정 코드를 올바르게 추출한다")
    void resolveAdjustmentAccountCodesSuccess() {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setId(10L);
        unit.setCriteriaJson("{\"adjustmentDebitAccountCode\":\"131000\",\"adjustmentCreditAccountCode\":\"211000\"}");

        ReconciliationAdjustmentPolicy.AdjustmentAccountCodes codes = policy.resolveAdjustmentAccountCodes(unit);

        assertThat(codes.debitAccountCode()).isEqualTo("131000");
        assertThat(codes.creditAccountCode()).isEqualTo("211000");
    }

    @Test
    @DisplayName("criteriaJson에 계정 코드가 누락된 경우 IllegalArgumentException이 발생한다")
    void resolveAdjustmentAccountCodesThrowsWhenMissing() {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setId(10L);
        unit.setCriteriaJson("{\"sourceAmount\":\"1000.00\"}");

        assertThatThrownBy(() -> policy.resolveAdjustmentAccountCodes(unit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires adjustmentDebitAccountCode and adjustmentCreditAccountCode");
    }

    @Test
    @DisplayName("이미 조정 전표가 발행된 경우 canGenerateAdjustment는 false를 반환하여 중복 발행을 차단한다")
    void canGenerateAdjustmentEnforcesIdempotency() {
        assertThat(policy.canGenerateAdjustment(true)).isFalse();
        assertThat(policy.canGenerateAdjustment(false)).isTrue();
    }

    @Test
    @DisplayName("buildLogicalPeriodKey는 단위 ID와 기준일로 일관된 논리 기간 키를 생성한다")
    void buildLogicalPeriodKey() {
        String key = policy.buildLogicalPeriodKey(10L, LocalDate.of(2026, 5, 11));
        assertThat(key).isEqualTo("UNIT-10-DATE-2026-05-11");
    }

    @Test
    @DisplayName("buildAdjustmentSourceDocumentId는 멱등적 추적이 가능한 고유 lineageSourceId를 생성한다")
    void buildAdjustmentSourceDocumentId() {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setId(10L);
        ReconciliationRun run = ReconciliationRun.startRun(unit, LocalDate.of(2026, 5, 11), "SYSTEM");
        run.setId(900L);

        ReconciliationDifference diff = new ReconciliationDifference();
        diff.setId(901L);
        diff.setDifferenceType(ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH);

        ReconciliationAdjustmentPolicy.AdjustmentAccountCodes codes =
                new ReconciliationAdjustmentPolicy.AdjustmentAccountCodes("131000", "211000");

        String sourceDocId = policy.buildAdjustmentSourceDocumentId(
                run, diff, LocalDate.of(2026, 5, 11), new BigDecimal("50.00"), codes);

        assertThat(sourceDocId).isEqualTo("RECON_ADJ-RUN-900-DIFF-901-DR-131000-CR-211000");
    }
}
