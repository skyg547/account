package com.ho.account.deposit.service;

import com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy;
import com.ho.account.deposit.domain.DepositDayCountConvention;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * [도메인 계산기] 예금 중도해지/만기해지 정산 및 세금 원천징수 계산 엔진.
 *
 * 💡 [초보자를 위한 금융 업무 설명]
 * 고객이 예금을 만기 시 해지하거나 중간에 중도 해지할 때:
 * 1. 약정이율 또는 중도해지 패널티 이율을 적용하여 세전 이자를 계산합니다.
 * 2. 한국 금융 세법 기준 이자소득세 14% + 지방소득세 1.4% = 총 15.4%를 원천징수(Withholding Tax)합니다.
 * 3. 최종 지급액 = 예금 원금 + 세후 이자(세전 이자 - 원천징수 세금).
 *
 * 📌 [세금 및 정산 수식]
 * - 세전 이자 = \( \text{원금} \times \text{적용이율} \times \frac{\text{경과일수}}{\text{365}} \)
 * - 이자소득세 = 세전 이자 × 0.14 (절사/반올림)
 * - 지방소득세 = 세전 이자 × 0.014 (절사/반올림)
 * - 세후 이자 = 세전 이자 - (이자소득세 + 지방소득세)
 * - 최종 실지급액 = 원금 + 세후 이자
 */
@Component
public class DepositTerminationSettlementCalculator {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final BigDecimal INCOME_TAX_RATE = new BigDecimal("0.1400"); // 14.0% 이자소득세
    private static final BigDecimal LOCAL_TAX_RATE = new BigDecimal("0.0140");  // 1.4% 지방소득세

    private final DepositInterestAccrualCalculator accrualCalculator;

    public DepositTerminationSettlementCalculator(DepositInterestAccrualCalculator accrualCalculator) {
        this.accrualCalculator = accrualCalculator;
    }

    /**
     * 만기 해지 정산 계산 (기본 원화 절사 정책)
     */
    public DepositTerminationResult calculateMaturitySettlement(
            String accountNumber,
            BigDecimal principal,
            BigDecimal agreedAnnualRate,
            LocalDate openDate,
            LocalDate maturityDate,
            DepositDayCountConvention convention) {

        return calculateSettlement(
                accountNumber, principal, agreedAnnualRate, openDate, maturityDate, maturityDate, false, convention, CurrencyTaxRoundingPolicy.KRW);
    }

    /**
     * 만기 해지 정산 계산 (다통화 세금 절사/반올림 정책 적용)
     */
    public DepositTerminationResult calculateMaturitySettlement(
            String accountNumber,
            BigDecimal principal,
            BigDecimal agreedAnnualRate,
            LocalDate openDate,
            LocalDate maturityDate,
            DepositDayCountConvention convention,
            CurrencyTaxRoundingPolicy policy) {

        return calculateSettlement(
                accountNumber, principal, agreedAnnualRate, openDate, maturityDate, maturityDate, false, convention, policy);
    }

    /**
     * 중도 해지 정산 계산 (기본 원화 절사 정책)
     */
    public DepositTerminationResult calculateEarlyTerminationSettlement(
            String accountNumber,
            BigDecimal principal,
            BigDecimal agreedAnnualRate,
            LocalDate openDate,
            LocalDate maturityDate,
            LocalDate terminationDate,
            DepositDayCountConvention convention) {

        return calculateSettlement(
                accountNumber, principal, agreedAnnualRate, openDate, maturityDate, terminationDate, true, convention, CurrencyTaxRoundingPolicy.KRW);
    }

    /**
     * 중도 해지 정산 계산 (다통화 세금 절사/반올림 정책 적용)
     */
    public DepositTerminationResult calculateEarlyTerminationSettlement(
            String accountNumber,
            BigDecimal principal,
            BigDecimal agreedAnnualRate,
            LocalDate openDate,
            LocalDate maturityDate,
            LocalDate terminationDate,
            DepositDayCountConvention convention,
            CurrencyTaxRoundingPolicy policy) {

        return calculateSettlement(
                accountNumber, principal, agreedAnnualRate, openDate, maturityDate, terminationDate, true, convention, policy);
    }

    private DepositTerminationResult calculateSettlement(
            String accountNumber,
            BigDecimal principal,
            BigDecimal agreedAnnualRate,
            LocalDate openDate,
            LocalDate maturityDate,
            LocalDate settlementDate,
            boolean isEarlyTermination,
            DepositDayCountConvention convention,
            CurrencyTaxRoundingPolicy policy) {

        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive.");
        }
        if (agreedAnnualRate == null || agreedAnnualRate.signum() <= 0 || agreedAnnualRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("agreedAnnualRate must be between 0 and 1.");
        }
        if (openDate == null || settlementDate == null || settlementDate.isBefore(openDate)) {
            throw new IllegalArgumentException("settlementDate must be on or after openDate.");
        }

        com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy safePolicy = (policy != null) ? policy : com.ho.account.deposit.domain.CurrencyTaxRoundingPolicy.KRW;

        long totalTermDays = Math.max(1, ChronoUnit.DAYS.between(openDate, maturityDate != null ? maturityDate : settlementDate));
        long elapsedDays = Math.max(0, ChronoUnit.DAYS.between(openDate, settlementDate));

        BigDecimal appliedRate = agreedAnnualRate;
        if (isEarlyTermination) {
            // 중도해지 패널티 비율 산출 (경과 비율에 따른 차등 적용)
            BigDecimal elapsedRatio = BigDecimal.valueOf(elapsedDays).divide(BigDecimal.valueOf(totalTermDays), MC);
            BigDecimal penaltyFactor = calculatePenaltyFactor(elapsedRatio);
            appliedRate = agreedAnnualRate.multiply(penaltyFactor, MC).setScale(8, RoundingMode.HALF_UP);
        }

        BigDecimal grossInterest = accrualCalculator.calculateDailyAccrual(principal, appliedRate, elapsedDays, convention)
                .setScale(2, RoundingMode.HALF_UP);

        // 원천징수 세금 산출 (통화별 절사/반올림 정책 적용)
        BigDecimal incomeTax = safePolicy.applyRounding(grossInterest.multiply(INCOME_TAX_RATE, MC));
        BigDecimal localIncomeTax = safePolicy.applyRounding(grossInterest.multiply(LOCAL_TAX_RATE, MC));
        BigDecimal totalTaxWithheld = incomeTax.add(localIncomeTax, MC).setScale(2, RoundingMode.HALF_UP);

        BigDecimal netInterest = grossInterest.subtract(totalTaxWithheld, MC).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayoutAmount = principal.add(netInterest, MC).setScale(2, RoundingMode.HALF_UP);

        return new DepositTerminationResult(
                accountNumber,
                isEarlyTermination,
                settlementDate,
                principal.setScale(2, RoundingMode.HALF_UP),
                grossInterest,
                appliedRate,
                incomeTax,
                localIncomeTax,
                totalTaxWithheld,
                netInterest,
                netPayoutAmount
        );
    }

    /**
     * 경과 비율에 따른 중도해지 패널티 적용 비율 산출
     */
    private BigDecimal calculatePenaltyFactor(BigDecimal elapsedRatio) {
        if (elapsedRatio.compareTo(new BigDecimal("0.90")) >= 0) {
            return new BigDecimal("0.80"); // 90% 이상 경과 시 약정이율의 80% 적용
        } else if (elapsedRatio.compareTo(new BigDecimal("0.50")) >= 0) {
            return new BigDecimal("0.50"); // 50% 이상 경과 시 약정이율의 50% 적용
        } else {
            return new BigDecimal("0.30"); // 50% 미만 경과 시 약정이율의 30% 적용
        }
    }
}
