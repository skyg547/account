package com.ho.account.deposit.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [도메인 VO] 예금 해지 정산 결과 불변 객체.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 예금이 만기해지되거나 중도해지될 때, 차주(예금주)에게 지급할 세전 이자, 원천징수 세금(이자소득세 + 지방소득세), 세후 이자, 최종 지급총액을 담는 불변 객체입니다.
 */
public record DepositTerminationResult(
        String accountNumber,
        boolean isEarlyTermination,
        LocalDate settlementDate,
        BigDecimal principalAmount,
        BigDecimal grossInterest,
        BigDecimal appliedInterestRate,
        BigDecimal incomeTax,
        BigDecimal localIncomeTax,
        BigDecimal totalTaxWithheld,
        BigDecimal netInterest,
        BigDecimal netPayoutAmount
) {
    public DepositTerminationResult {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber is required.");
        }
        if (settlementDate == null) {
            throw new IllegalArgumentException("settlementDate is required.");
        }
        if (principalAmount == null || principalAmount.signum() < 0) {
            throw new IllegalArgumentException("principalAmount must be zero or positive.");
        }
        if (grossInterest == null || grossInterest.signum() < 0) {
            throw new IllegalArgumentException("grossInterest must be zero or positive.");
        }
        if (appliedInterestRate == null || appliedInterestRate.signum() < 0) {
            throw new IllegalArgumentException("appliedInterestRate must be zero or positive.");
        }
        if (incomeTax == null || incomeTax.signum() < 0) {
            throw new IllegalArgumentException("incomeTax must be zero or positive.");
        }
        if (localIncomeTax == null || localIncomeTax.signum() < 0) {
            throw new IllegalArgumentException("localIncomeTax must be zero or positive.");
        }
        if (totalTaxWithheld == null || totalTaxWithheld.signum() < 0) {
            throw new IllegalArgumentException("totalTaxWithheld must be zero or positive.");
        }
        if (netInterest == null || netInterest.signum() < 0) {
            throw new IllegalArgumentException("netInterest must be zero or positive.");
        }
        if (netPayoutAmount == null || netPayoutAmount.signum() < 0) {
            throw new IllegalArgumentException("netPayoutAmount must be zero or positive.");
        }
    }
}
