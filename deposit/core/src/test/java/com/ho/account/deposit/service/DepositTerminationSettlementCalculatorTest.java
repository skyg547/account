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

    @Test
    @DisplayName("USD 외화 수신 계좌 1,000달러 만기해지 시 센트(0.01) 단위 원천징수 15.4% 세금이 올바르게 반올림 계산된다")
    void calculateMaturitySettlement_USD() {
        BigDecimal principal = new BigDecimal("1000.00");
        BigDecimal agreedRate = new BigDecimal("0.0500"); // 연 5.0%
        LocalDate openDate = LocalDate.of(2025, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 1, 1);

        DepositTerminationResult result = settlementCalculator.calculateMaturitySettlement(
                "DEP-USD-001", principal, agreedRate, openDate, maturityDate, DepositDayCountConvention.ACTUAL_365,
                com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.USD);

        assertThat(result.isEarlyTermination()).isFalse();
        // 세전 이자: 1,000 * 0.05 * 1 = 50.00 달러
        assertThat(result.grossInterest()).isEqualByComparingTo("50.00");
        // 이자소득세 (14%): 50.00 * 0.14 = 7.00 달러
        assertThat(result.incomeTax()).isEqualByComparingTo("7.00");
        // 지방소득세 (1.4%): 50.00 * 0.014 = 0.70 달러
        assertThat(result.localIncomeTax()).isEqualByComparingTo("0.70");
        // 총 원천징수: 7.70 달러
        assertThat(result.totalTaxWithheld()).isEqualByComparingTo("7.70");
        // 세후 이자: 42.30 달러
        assertThat(result.netInterest()).isEqualByComparingTo("42.30");
        // 실지급액: 1,042.30 달러
        assertThat(result.netPayoutAmount()).isEqualByComparingTo("1042.30");
    }

    @Test
    @DisplayName("CurrencyTaxRoundingPolicy factory method of()로 통화 코드 매핑이 정상 동작한다")
    void currencyTaxRoundingPolicy_of() {
        assertThat(com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.of("USD"))
                .isEqualTo(com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.USD);
        assertThat(com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.of("eur"))
                .isEqualTo(com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.EUR);
        assertThat(com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.of("JPY"))
                .isEqualTo(com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.JPY);
        assertThat(com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.of("UNKNOWN"))
                .isEqualTo(com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.KRW);
    }
}
