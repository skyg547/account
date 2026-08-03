package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.shared.finance.enums.CrStaging;
import org.springframework.stereotype.Component;

/**
 * [도메인 계산기] IFRS 9 순수 스테이징(Staging) 판정 코어 엔진.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 클래스는 외부 프레임워크(Spring DB/Repository)에 의존하지 않는 pure Java 도메인 계산기입니다.
 * IFRS 9 국제회계기준의 SICR(Significant Increase in Credit Risk, 신용위험의 유의미한 증가) 규칙을
 * 객체지향 도메인 규칙으로 집약하여 평가합니다.
 *
 * 📌 [스테이징 3단계 평가 규칙]
 * 1. Stage 3 (Credit Impaired / Default / 손상)
 *    - 연체일수 >= 90일
 *    - 채무조정(Debt Restructuring) 대상
 *    - 부도/파산 상태
 *
 * 2. Stage 2 (SICR / Significant Increase in Credit Risk / 주의)
 *    - 연체일수 >= 30일 및 < 90일
 *    - 최초 실행 대비 등급 하락폭(Notch Downgrade) >= 3 노치
 *    - 조기경보 등급이 'CRITICAL' 또는 'WARNING'
 *
 * 3. Stage 1 (Performing / Normal / 정상)
 *    - 위 Stage 2, Stage 3 조건에 해당하지 않는 정상 익스포저
 */
@Component
public class IfrsStagingEngine {

    /**
     * 계좌 및 차주의 제반 신용 상태를 종합 평가하여 IFRS 9 스테이징 결과를 산출합니다.
     *
     * @param delinquentDays     평가 대상 연체 일수
     * @param originalRank       대출 최초 실행 시점 등급 랭크 (숫자가 클수록 낮은 등급, 예: AAA=1, D=20)
     * @param currentRank        현재 평가 시점 등급 랭크
     * @param warningLevel       조기경보 등급 (null, "NORMAL", "WARNING", "CRITICAL")
     * @param isDebtRestructured 채무조정(채권재조정) 여부
     * @return 평가된 Stage, 판정 사유 및 세부 지표가 담긴 불변 결과 객체
     */
    public StagingDecisionResult evaluateStaging(
            int delinquentDays,
            int originalRank,
            int currentRank,
            String warningLevel,
            boolean isDebtRestructured) {

        int safeDelinquentDays = Math.max(0, delinquentDays);
        String safeWarningLevel = warningLevel != null ? warningLevel.toUpperCase() : "NORMAL";
        int notchDowngrade = Math.max(0, currentRank - originalRank);

        // ──────────────────────────────────────────
        // 1. Stage 3 (손상 / Credit Impaired) 판정
        // ──────────────────────────────────────────
        if (safeDelinquentDays >= 90) {
            return StagingDecisionResult.builder()
                    .stage(CrStaging.STAGE3)
                    .primaryTrigger("DELINQUENCY_GE_90_DAYS")
                    .delinquentDays(safeDelinquentDays)
                    .notchDowngrade(notchDowngrade)
                    .warningLevel(safeWarningLevel)
                    .isDebtRestructured(isDebtRestructured)
                    .build();
        }

        if (isDebtRestructured) {
            return StagingDecisionResult.builder()
                    .stage(CrStaging.STAGE3)
                    .primaryTrigger("DEBT_RESTRUCTURED")
                    .delinquentDays(safeDelinquentDays)
                    .notchDowngrade(notchDowngrade)
                    .warningLevel(safeWarningLevel)
                    .isDebtRestructured(true)
                    .build();
        }

        // ──────────────────────────────────────────
        // 2. Stage 2 (주의 / SICR) 판정
        // ──────────────────────────────────────────
        if (safeDelinquentDays >= 30) {
            return StagingDecisionResult.builder()
                    .stage(CrStaging.STAGE2)
                    .primaryTrigger("DELINQUENCY_GE_30_DAYS")
                    .delinquentDays(safeDelinquentDays)
                    .notchDowngrade(notchDowngrade)
                    .warningLevel(safeWarningLevel)
                    .isDebtRestructured(false)
                    .build();
        }

        if ("CRITICAL".equals(safeWarningLevel)) {
            return StagingDecisionResult.builder()
                    .stage(CrStaging.STAGE2)
                    .primaryTrigger("EARLY_WARNING_CRITICAL")
                    .delinquentDays(safeDelinquentDays)
                    .notchDowngrade(notchDowngrade)
                    .warningLevel(safeWarningLevel)
                    .isDebtRestructured(false)
                    .build();
        }

        if (notchDowngrade >= 3) {
            return StagingDecisionResult.builder()
                    .stage(CrStaging.STAGE2)
                    .primaryTrigger("NOTCH_DOWNGRADE_GE_3")
                    .delinquentDays(safeDelinquentDays)
                    .notchDowngrade(notchDowngrade)
                    .warningLevel(safeWarningLevel)
                    .isDebtRestructured(false)
                    .build();
        }

        if ("WARNING".equals(safeWarningLevel)) {
            return StagingDecisionResult.builder()
                    .stage(CrStaging.STAGE2)
                    .primaryTrigger("EARLY_WARNING_WARNING")
                    .delinquentDays(safeDelinquentDays)
                    .notchDowngrade(notchDowngrade)
                    .warningLevel(safeWarningLevel)
                    .isDebtRestructured(false)
                    .build();
        }

        // ──────────────────────────────────────────
        // 3. Stage 1 (정상 / Performing)
        // ──────────────────────────────────────────
        return StagingDecisionResult.builder()
                .stage(CrStaging.STAGE1)
                .primaryTrigger("PERFORMING_NORMAL")
                .delinquentDays(safeDelinquentDays)
                .notchDowngrade(notchDowngrade)
                .warningLevel(safeWarningLevel)
                .isDebtRestructured(false)
                .build();
    }
}
