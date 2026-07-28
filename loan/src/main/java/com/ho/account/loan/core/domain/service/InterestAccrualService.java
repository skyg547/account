package com.ho.account.loan.core.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 대출(Loan) 도메인의 핵심 비즈니스 로직인 '일할 이자 발생(Daily Accrual)'을 담당하는 도메인 서비스입니다.
 * 
 * <p><b>초보자 가이드(Beginner's Guide):</b></p>
 * <p>
 * 은행은 고객이 대출 이자를 아직 내지 않았더라도, 매일매일 발생하는 이자를 장부에 기록해야 합니다.
 * 이를 '발생주의 회계(Accrual Accounting)'라고 합니다.
 * 매일 EOD(End of Day) 시점마다 배치(Batch) 프로그램이 이 도메인 서비스를 호출하여,
 * 미수이자(Accrued Interest Receivable)를 계산하고 원장에 반영합니다.
 * </p>
 * 
 * <p><b>헥사고날 아키텍처 관점(Hexagonal Architecture):</b></p>
 * <p>
 * 이 클래스는 Core Domain Layer에 위치하므로, JPA Entity나 DB 기술, HTTP 등에 절대 의존하지 않습니다.
 * 순수 Java 로직으로 구성되며, 복잡한 금융 수학(BigDecimal)과 비즈니스 규칙만을 응집도 있게 관리합니다.
 * </p>
 */
public class InterestAccrualService {

    /**
     * 1년을 365일로 가정하는 이자 계산 일수 상수.
     * (실제 은행에서는 윤년, 360일 기준(ACT/360) 등 다양한 이수계산(Day Count) 관행을 지원해야 하나 단순화함)
     */
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    /**
     * 하루치 대출 이자를 계산합니다.
     * 
     * @param principalBalance 현재 대출 잔액 (원금)
     * @param annualInterestRate 연 이자율 (예: 0.05 -> 5%)
     * @param accrualDate 발생일자 (보통 오늘 EOD 일자)
     * @return 계산된 하루치 이자 금액
     */
    public BigDecimal calculateDailyAccrual(BigDecimal principalBalance, BigDecimal annualInterestRate, LocalDate accrualDate) {
        if (principalBalance == null || principalBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        /*
         * [이자 계산 공식]
         * 일일 이자 = 잔액 * 연이율 / 365
         * 
         * 주의: 금융 도메인에서는 float, double 절대 금지! 부동소수점 오차로 인해 금액이 틀려집니다.
         * 반드시 BigDecimal을 사용하고 스케일(소수점) 처리를 명확하게 지정해야 합니다.
         */
        BigDecimal yearlyInterest = principalBalance.multiply(annualInterestRate);
        
        // 소수점 이하는 절사(버림, DOWN) 처리. 국가 및 통화 정책마다 ROUND_HALF_UP(반올림)을 쓸 수도 있습니다.
        BigDecimal dailyInterest = yearlyInterest.divide(DAYS_IN_YEAR, 0, RoundingMode.DOWN);

        return dailyInterest;
    }

    /**
     * 특정 기간 동안의 이자를 한 번에 발생시켜야 할 때 사용 (주로 휴일 롤오버 등)
     */
    public BigDecimal calculatePeriodAccrual(BigDecimal principalBalance, BigDecimal annualInterestRate, int days) {
        BigDecimal daily = calculateDailyAccrual(principalBalance, annualInterestRate, LocalDate.now());
        return daily.multiply(BigDecimal.valueOf(days));
    }
}
