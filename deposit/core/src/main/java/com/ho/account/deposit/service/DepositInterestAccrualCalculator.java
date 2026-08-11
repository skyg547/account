package com.ho.account.deposit.service;

import com.ho.account.deposit.domain.DepositDayCountConvention;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
/**
 * [순수 도메인 계산기 (Pure Domain Service)] 약정이율 기반 예금 이자 일할 계산(Accrual) 코어 엔진.
 *
 * 💡 [DDD 원칙 - 도메인 서비스의 순수성 (Pure Domain Service)]
 * 이 클래스는 복잡한 금융 수식 연산을 담당하는 도메인 서비스(Domain Service)입니다.
 * `@Component`와 같은 스프링 어노테이션을 배제하고 순수한 Java POJO로 작성되어 있습니다.
 * - **결합도 분리**: 도메인 로직이 기술적 인프라(스프링 IoC 등)에 종속되지 않고 영구히 보존됩니다.
 * - **단위 테스트 효율성**: 스프링 컨테이너의 도움 없이 즉시 객체를 생성하여 빠르게 검증할 수 있습니다.
 *
 * 💡 [초보자를 위한 금융 수학 및 회계 수식 설명]
 * 이 계산기는 예금 잔액, 약정 연 이율, 경과 일수에 따라 매일 발생하는 미지급 이자(Accrued Interest)를 정밀 산출합니다.
 *
 * 📌 [일할 계산 수식]
 * \[ \text{Accrued Interest} = \text{Balance} \times \text{Annual Rate} \times \frac{\text{Elapsed Days}}{\text{Days Per Year (365 or 360)}} \]
 *
 * 🔧 [금융 정밀도 정책]
 * - 내부 연산 정밀도: `MathContext(34, RoundingMode.HALF_EVEN)`
 * - 최종 미지급 이자 금액: `setScale(4, RoundingMode.HALF_UP)` (소수점 이하 4자리 보존)
 */
public class DepositInterestAccrualCalculator {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);

    /**
     * 지정된 잔액, 약정이율 및 경과일수에 따른 이자를 정밀 계산합니다.
     *
     * @param balance       예금 잔액
     * @param annualRate    약정 연 이자율 (예: 0.0350 = 3.5%)
     * @param elapsedDays   이자 산출 경과 일수
     * @param convention    일수 계산 기준 (ACTUAL_365, ACTUAL_360)
     * @return 계산된 일할 미지급 이자 금액 (scale = 4)
     */
    public BigDecimal calculateDailyAccrual(
            BigDecimal balance,
            BigDecimal annualRate,
            long elapsedDays,
            DepositDayCountConvention convention) {

        if (balance == null || balance.signum() <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (annualRate == null || annualRate.signum() <= 0 || annualRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("annualRate must be a positive decimal rate between 0 and 1.");
        }
        if (elapsedDays < 0) {
            throw new IllegalArgumentException("elapsedDays must not be negative.");
        }

        DepositDayCountConvention safeConvention = (convention != null) ? convention : DepositDayCountConvention.ACTUAL_365;
        BigDecimal daysPerYear = BigDecimal.valueOf(safeConvention.getDaysPerYear());

        // Daily Interest = Balance * AnnualRate * (elapsedDays / daysPerYear)
        BigDecimal daysFactor = BigDecimal.valueOf(elapsedDays).divide(daysPerYear, MC);
        BigDecimal interest = balance.multiply(annualRate, MC).multiply(daysFactor, MC);

        return interest.setScale(4, RoundingMode.HALF_UP);
    }
}
