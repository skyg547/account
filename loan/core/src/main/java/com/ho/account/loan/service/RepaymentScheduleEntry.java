package com.ho.account.loan.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [도메인 VO] 단일 회차 대출 상환 스케줄 항목 불변 객체.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 객체는 특정 회차(예: 1회차, 2회차...)에 차주가 납부할 기초잔액, 원금상환액, 약정이자, 총상환액, 기말잔액을 담는 불변 객체입니다.
 */
public record RepaymentScheduleEntry(
        int periodIndex,
        LocalDate paymentDate,
        BigDecimal beginningBalance,
        BigDecimal principalPayment,
        BigDecimal interestPayment,
        BigDecimal totalPayment,
        BigDecimal endingBalance
) {
    public RepaymentScheduleEntry {
        if (periodIndex <= 0) {
            throw new IllegalArgumentException("periodIndex must be positive.");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("paymentDate is required.");
        }
        if (beginningBalance == null || beginningBalance.signum() < 0) {
            throw new IllegalArgumentException("beginningBalance must be zero or positive.");
        }
        if (principalPayment == null || principalPayment.signum() < 0) {
            throw new IllegalArgumentException("principalPayment must be zero or positive.");
        }
        if (interestPayment == null || interestPayment.signum() < 0) {
            throw new IllegalArgumentException("interestPayment must be zero or positive.");
        }
        if (totalPayment == null || totalPayment.signum() < 0) {
            throw new IllegalArgumentException("totalPayment must be zero or positive.");
        }
        if (endingBalance == null || endingBalance.signum() < 0) {
            throw new IllegalArgumentException("endingBalance must be zero or positive.");
        }
    }
}
