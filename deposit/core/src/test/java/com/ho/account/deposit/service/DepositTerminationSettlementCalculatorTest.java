package com.ho.account.deposit.service;

import com.ho.account.deposit.domain.DepositDayCountConvention;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DepositTerminationSettlementCalculator 단위 테스트")
class DepositTerminationSettlementCalculatorTest {

    private final DepositInterestAccrualCalculator accrualCalculator = new DepositInterestAccrualCalculator();
    private final DepositTerminationSettlementCalculator settlementCalculator = new DepositTerminationSettlementCalculator(accrualCalculator);

    @Test
    @DisplayName("10,000,000원 1년 만기해지 시 세전이자, 원천징수 15.4% 세금 및 최종 지급액이 바르게 계산되는지 검증한다")
    void calculateMaturitySettlement() {
        BigDecimal principal = new BigDecimal("10000000.00");
        BigDecimal agreedRate = new BigDecimal("0.0400"); // 연 4.0%
        LocalDate openDate = LocalDate.of(2025, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 1, 1); // 365일 만기

        DepositTerminationResult result = settlementCalculator.calculateMaturitySettlement(
                "DEP-001", principal, agreedRate, openDate, maturityDate, DepositDayCountConvention.ACTUAL_365);

        assertThat(result.isEarlyTermination()).isFalse();
        // 세전 이자: 10,000,000 * 0.04 * (365/365) = 400,000.00
        assertThat(result.grossInterest()).isEqualByComparingTo("400000.00");
        // 이자소득세 (14%): 400,000 * 0.14 = 56,000.00
        assertThat(result.incomeTax()).isEqualByComparingTo("56000.00");
        // 지방소득세 (1.4%): 400,000 * 0.014 = 5,600.00
        assertThat(result.localIncomeTax()).isEqualByComparingTo("5600.00");
        // 총 원천징수: 61,600.00
        assertThat(result.totalTaxWithheld()).isEqualByComparingTo("61600.00");
        // 세후 이자: 338,400.00
        assertThat(result.netInterest()).isEqualByComparingTo("338400.00");
        // 최종 실지급액: 10,338,400.00
        assertThat(result.netPayoutAmount()).isEqualByComparingTo("10338400.00");
    }

    @Test
    @DisplayName("6개월(50% 이상 경과) 시점 중도해지 시 50% 패널티 이율 및 세금이 적용되어 정산되는지 검증한다")
    void calculateEarlyTerminationSettlement() {
        BigDecimal principal = new BigDecimal("10000000.00");
        BigDecimal agreedRate = new BigDecimal("0.0400"); // 연 4.0%
        LocalDate openDate = LocalDate.of(2025, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 1, 1);
        LocalDate terminationDate = LocalDate.of(2025, 7, 3); // 183일 경과 (>= 50% 경과)

        DepositTerminationResult result = settlementCalculator.calculateEarlyTerminationSettlement(
                "DEP-002", principal, agreedRate, openDate, maturityDate, terminationDate, DepositDayCountConvention.ACTUAL_365);

        assertThat(result.isEarlyTermination()).isTrue();
        // 적용 패널티 이율: 4.0% * 50% = 2.0% (0.0200)
        assertThat(result.appliedInterestRate()).isEqualByComparingTo("0.02000000");
        assertThat(result.netPayoutAmount()).isGreaterThan(principal);
    }
}
