package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.shared.finance.enums.CrStaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IfrsStagingEngineTest {

    private IfrsStagingEngine stagingEngine;

    @BeforeEach
    void setUp() {
        stagingEngine = new IfrsStagingEngine();
    }

    @Test
    @DisplayName("정상 건(연체 0일, 등급변동 없음, 정상 경보)은 Stage 1로 분류된다.")
    void evaluateStaging_Stage1_NormalExposure() {
        StagingDecisionResult result = stagingEngine.evaluateStaging(0, 5, 5, "NORMAL", false);

        assertThat(result.getStage()).isEqualTo(CrStaging.STAGE1);
        assertThat(result.getPrimaryTrigger()).isEqualTo("PERFORMING_NORMAL");
        assertThat(result.getDelinquentDays()).isEqualTo(0);
        assertThat(result.getNotchDowngrade()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(ints = {90, 91, 120, 365})
    @DisplayName("연체일수가 90일 이상인 자산은 Stage 3 (손상)으로 분류된다.")
    void evaluateStaging_Stage3_DelinquencyGe90(int delinquentDays) {
        StagingDecisionResult result = stagingEngine.evaluateStaging(delinquentDays, 5, 5, "NORMAL", false);

        assertThat(result.getStage()).isEqualTo(CrStaging.STAGE3);
        assertThat(result.getPrimaryTrigger()).isEqualTo("DELINQUENCY_GE_90_DAYS");
        assertThat(result.getDelinquentDays()).isEqualTo(delinquentDays);
    }

    @Test
    @DisplayName("채무조정(Debt Restructured) 대상 자산은 연체일수와 무관하게 Stage 3으로 분류된다.")
    void evaluateStaging_Stage3_DebtRestructured() {
        StagingDecisionResult result = stagingEngine.evaluateStaging(10, 5, 5, "NORMAL", true);

        assertThat(result.getStage()).isEqualTo(CrStaging.STAGE3);
        assertThat(result.getPrimaryTrigger()).isEqualTo("DEBT_RESTRUCTURED");
        assertThat(result.isDebtRestructured()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 45, 89})
    @DisplayName("연체일수가 30일 이상 90일 미만인 자산은 Stage 2 (SICR)로 분류된다.")
    void evaluateStaging_Stage2_DelinquencyGe30(int delinquentDays) {
        StagingDecisionResult result = stagingEngine.evaluateStaging(delinquentDays, 5, 5, "NORMAL", false);

        assertThat(result.getStage()).isEqualTo(CrStaging.STAGE2);
        assertThat(result.getPrimaryTrigger()).isEqualTo("DELINQUENCY_GE_30_DAYS");
    }

    @Test
    @DisplayName("조기경보가 CRITICAL인 자산은 Stage 2로 분류된다.")
    void evaluateStaging_Stage2_EarlyWarningCritical() {
        StagingDecisionResult result = stagingEngine.evaluateStaging(0, 5, 5, "CRITICAL", false);

        assertThat(result.getStage()).isEqualTo(CrStaging.STAGE2);
        assertThat(result.getPrimaryTrigger()).isEqualTo("EARLY_WARNING_CRITICAL");
    }

    @Test
    @DisplayName("신용등급 하락폭(Notch Downgrade)이 3이상인 자산은 Stage 2로 분류된다.")
    void evaluateStaging_Stage2_NotchDowngradeGe3() {
        // originalRank = 3 (AAA), currentRank = 6 (BBB) -> 차이 3 노치 하락
        StagingDecisionResult result = stagingEngine.evaluateStaging(0, 3, 6, "NORMAL", false);

        assertThat(result.getStage()).isEqualTo(CrStaging.STAGE2);
        assertThat(result.getPrimaryTrigger()).isEqualTo("NOTCH_DOWNGRADE_GE_3");
        assertThat(result.getNotchDowngrade()).isEqualTo(3);
    }

    @Test
    @DisplayName("조기경보가 WARNING인 자산은 Stage 2로 분류된다.")
    void evaluateStaging_Stage2_EarlyWarningWarning() {
        StagingDecisionResult result = stagingEngine.evaluateStaging(0, 5, 5, "WARNING", false);

        assertThat(result.getStage()).isEqualTo(CrStaging.STAGE2);
        assertThat(result.getPrimaryTrigger()).isEqualTo("EARLY_WARNING_WARNING");
    }

    @Test
    @DisplayName("경계조건: 연체 29일은 Stage 1, 30일은 Stage 2로 명확히 나뉜다.")
    void evaluateStaging_Boundary_Delinquency29vs30() {
        StagingDecisionResult day29 = stagingEngine.evaluateStaging(29, 5, 5, "NORMAL", false);
        StagingDecisionResult day30 = stagingEngine.evaluateStaging(30, 5, 5, "NORMAL", false);

        assertThat(day29.getStage()).isEqualTo(CrStaging.STAGE1);
        assertThat(day30.getStage()).isEqualTo(CrStaging.STAGE2);
    }

    @Test
    @DisplayName("경계조건: 연체 89일은 Stage 2, 90일은 Stage 3으로 명확히 나뉜다.")
    void evaluateStaging_Boundary_Delinquency89vs90() {
        StagingDecisionResult day89 = stagingEngine.evaluateStaging(89, 5, 5, "NORMAL", false);
        StagingDecisionResult day90 = stagingEngine.evaluateStaging(90, 5, 5, "NORMAL", false);

        assertThat(day89.getStage()).isEqualTo(CrStaging.STAGE2);
        assertThat(day90.getStage()).isEqualTo(CrStaging.STAGE3);
    }
}
