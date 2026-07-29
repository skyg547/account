package com.ho.account.deposit.core.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 수신(Deposit) 도메인에서 예금에 대한 '일할 이자 발생(Daily Accrual)'을 담당하는 도메인 서비스입니다.
 * 
 * <p><b>초보자 가이드(Beginner's Guide):</b></p>
 * <p>
 * 고객이 은행에 예금을 맡기면, 은행은 고객에게 이자를 지급할 의무(부채)가 생깁니다.
 * 매일 자정(EOD)에 이 클래스가 호출되어, 고객에게 지급해야 할 하루치 이자를 계산합니다.
 * 이 금액은 미지급이자(Accrued Interest Payable)라는 부채 계정에 차곡차곡 쌓이다가,
 * 실제 이자 지급일(결산일)에 고객 계좌 원금으로 쏙 들어가게 됩니다.
 * 대출 이자 계산(InterestAccrualService)과 논리는 비슷하지만, 회계상 '비용'과 '부채'라는 점이 다릅니다.
 * </p>
 */
public class DepositAccrualService {

    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    /**
     * 하루치 예금 이자를 계산합니다.
     * 
     * @param depositBalance 현재 예금 잔액
     * @param annualInterestRate 연 이자율
     * @param accrualDate 발생일자 (EOD 기준일)
     * @return 계산된 하루치 예금 이자
     */
    public BigDecimal calculateDailyAccrual(BigDecimal depositBalance, BigDecimal annualInterestRate, LocalDate accrualDate) {
        if (depositBalance == null || depositBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal yearlyInterest = depositBalance.multiply(annualInterestRate);
        
        // 은행 입장에서 고객에게 불리하지 않게 소수점 반올림 처리(HALF_UP)를 하기도 함
        // (상품별 이자 계산 약관에 따라 절사/올림/반올림 적용이 다름)
        return yearlyInterest.divide(DAYS_IN_YEAR, 0, RoundingMode.HALF_UP);
    }
}
