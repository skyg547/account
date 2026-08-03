package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [도메인 서비스/계산기] 대사 차이(Reconciliation Difference) 해소 코어 엔진.
 *
 * 💡 [초보자를 위한 금융 업무 설명]
 * 대사(Reconciliation) 실행 후 원장 ↔ 외부은행 간 금액 불일치나 항목 누락 차이가 발굴되었을 때,
 * 차이의 원인(Reason Code)을 지정하고 필요시 조정 전표(Adjustment Journal)를 생성하여 차이를 `RESOLVED` 상태로 종결하는 코어 비즈니스 엔진입니다.
 *
 * 📌 [차이 해소 업무 규칙]
 * 1. 이미 `RESOLVED` 또는 `IGNORED` 상태인 차이는 재해소할 수 없습니다.
 * 2. 차이는 반드시 정당한 원인 사유 코드(`DifferenceReasonCode`)를 지정해야만 해지/종결 처리됩니다.
 * 3. 금액 불일치(`AMOUNT_MISMATCH`)나 누락 항목(`MISSING_SOURCE`/`MISSING_TARGET`)은 차액만큼의 조정 전표(Adjustment Entry)를 발행합니다.
 */
@Component
public class ReconciliationDiffResolver {

    /**
     * 대사 차이 항목에 사유 코드를 부여하고 조정 사유와 함께 해소(RESOLVED) 처리합니다.
     *
     * @param difference 해소할 대사 차이 엔티티
     * @param reasonCode 차이 원인 사유 코드 엔티티
     * @param resolutionMemo 해소 처리 사유 및 메모
     * @param resolverUser 해소 처리 담당자
     */
    public void resolveDifference(
            ReconciliationDifference difference,
            DifferenceReasonCode reasonCode,
            String resolutionMemo,
            String resolverUser) {

        if (difference == null) {
            throw new IllegalArgumentException("difference is required.");
        }
        if (reasonCode == null) {
            throw new IllegalArgumentException("reasonCode is required.");
        }
        if (resolverUser == null || resolverUser.isBlank()) {
            throw new IllegalArgumentException("resolverUser is required.");
        }

        ReconciliationDifference.ReconciliationDifferenceStatus currentStatus = difference.getStatus();
        if (currentStatus == ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED) {
            throw new IllegalStateException("Difference is already resolved.");
        }
        if (currentStatus == ReconciliationDifference.ReconciliationDifferenceStatus.IGNORED) {
            throw new IllegalStateException("Ignored difference cannot be resolved.");
        }

        difference.setReasonCode(reasonCode);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED);
        difference.setResolvedAt(LocalDateTime.now());
        difference.setResolvedBy(resolverUser.trim());

        if (resolutionMemo != null && !resolutionMemo.isBlank()) {
            String updatedDesc = (difference.getDescription() != null ? difference.getDescription() + " | Memo: " : "Memo: ")
                    + resolutionMemo.trim();
            difference.setDescription(updatedDesc);
        }
    }

    /**
     * 차이 해소를 위한 조정 전표 필요 금액(Adjustment Amount)을 계산합니다.
     *
     * @param difference 차이 항목
     * @return 조정 전표 발생 금액 (절대값)
     */
    public BigDecimal calculateAdjustmentAmount(ReconciliationDifference difference) {
        if (difference == null) {
            throw new IllegalArgumentException("difference is required.");
        }

        BigDecimal diff = difference.getDifferenceAmount();
        if (diff != null && diff.signum() != 0) {
            return diff.abs();
        }

        BigDecimal expected = (difference.getAmountExpected() != null) ? difference.getAmountExpected() : BigDecimal.ZERO;
        BigDecimal actual = (difference.getAmountActual() != null) ? difference.getAmountActual() : BigDecimal.ZERO;

        return expected.subtract(actual).abs();
    }
}
