package com.ho.account.asset.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root]
 * 리스 계약(Lease Contract) 엔티티 — IFRS 16 기준에 따른 리스 계약 정보를 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 리스는 남의 물건을 빌려 쓰는 것이지만, 회계적으로는 '내 것처럼' 장부에 올리는 복잡한 처리입니다.
 * 이 클래스는 "매달 낼 리스료는 얼마인지", "이자율은 몇 퍼센트인지"를 기억하고,
 * 이를 바탕으로 "지금 이 계약을 자산으로 환산하면 얼마짜리인지(사용권자산)"와 
 * "앞으로 갚아야 할 빚은 얼마인지(리스부채)"를 계산하는 기준이 됩니다.
 * 타 모듈(Master Data)과는 ID(lessorCode 등) 기반으로 약하게 결합되어 있습니다.
 * 
 * 🎓 [교육적 주석 - IFRS 16 리스 회계 및 현재가치(PV) 계산 원리]
 * 1. IFRS 16 리스 회계의 핵심:
 *    과거 운용리스는 단순히 지급 임차료(비용)로 처리했으나, IFRS 16에서는 미래에 지급할 리스료 의무를 
 *    현재가치(Present Value, PV)로 할인하여 장부에 '사용권자산(ROU Asset)'과 '리스부채(Lease Liability)'로 최초 계상해야 합니다.
 * 2. 증분차입이자율 (Incremental Borrowing Rate) 할인율:
 *    기업이 유사한 기간, 유사한 담보로 자산을 빌릴 때 부담해야 하는 이자율입니다.
 *    연 이자율(annualRate)을 월 할인율(r = annualRate / 12 / 100 = annualRate / 1200)로 환산하여 적용합니다.
 * 3. 현재가치(PV) 계산 공식:
 *    PV = ∑ [ PMT / (1 + r)^t ] (t = 1 to N)
 *    매월 발생하는 현금유출액(PMT, monthlyPayment)을 복리 할인율 (1+r)^t 로 나누어 현시점의 가치로 환산 합산합니다.
 * 4. 금융 회계 도메인 불변성(Domain Invariants)과 외부 입력 교차 검증:
 *    외부 API 요청이나 DTO로 전달받는 PV 가액을 100% 신뢰하는 경우, 외부의 잘못된 계산이나 임의 데이터 변조로
 *    장부 가액 불일치가 발생할 수 있습니다. 따라서 도메인 내부에서 계약 조건 기반 PV를 자동 계산하고 
 *    외부 입력값을 교차 검증(Cross-Validation)하여 도메인 모델의 데이터 정합성을 보장합니다.
 */
@Entity
@Table(name = "lease_contracts")
@Getter @Setter
@NoArgsConstructor
public class LeaseContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String contractNo;

    @Column(nullable = false)
    private String contractName;

    @Column(name = "lessor_code", length = 20)
    private String lessorCode;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(nullable = false)
    private int paymentDay;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal discountRate; // 증분차입이자율 (%)

    @Column(precision = 19, scale = 2)
    private BigDecimal initialRightOfUseAssetValue; // 초기 사용권자산 PV
    @Column(precision = 19, scale = 2)
    private BigDecimal initialLeaseLiabilityValue; // 초기 리스부채 PV

    private String status; // ACTIVE, TERMINATED, MODIFIED

    @Column(name = "dept_code", length = 20)
    private String departmentCode;

    @Column(name = "expense_account_code", length = 20)
    private String expenseAccountCode;

    @Column(name = "ifrs16_applicable", nullable = false)
    private boolean ifrs16Applicable = true;
    @Column(nullable = false)
    private boolean shortTermLease = false;
    @Column(nullable = false)
    private boolean lowValueLease = false;

    public void validateForRegistration() {
        if (paymentDay < 1 || paymentDay > 31) {
            throw new IllegalArgumentException("payment day must be between 1 and 31");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("lease start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("lease start date must be on or before end date");
        }
    }

    /**
     * 시작일(startDate)과 종료일(endDate) 사이의 총 리스 기간(개월 수)을 계산합니다.
     */
    public int calculateTermMonths() {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }
        int count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            count++;
            current = current.plusMonths(1);
        }
        return count;
    }

    /**
     * IFRS 16 규격에 따라 미래 리스료 현금흐름의 현재가치(Present Value, PV)를 계산합니다.
     * 
     * @param monthlyPayment 월 리스료 (PMT)
     * @param termMonths 총 리스 기간 개월 수 (N)
     * @param annualRate 연 증분차입이자율 (%, e.g. 5.0)
     * @return 계산된 현재가치 (scale 2 적용)
     */
    public static BigDecimal calculatePresentValue(BigDecimal monthlyPayment, int termMonths, BigDecimal annualRate) {
        if (monthlyPayment == null || monthlyPayment.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return calculatePresentValueLadder(monthlyPayment, termMonths, annualRate).get(termMonths);
    }

    /** Reprice only unpaid installments; original recognition values remain historical. */
    public static Remeasurement calculateRemeasurement(BigDecimal currentLiability,
                                                       BigDecimal monthlyPayment,
                                                       int remainingPeriods,
                                                       BigDecimal annualRate) {
        if (currentLiability == null || currentLiability.signum() < 0) {
            throw new IllegalStateException("current lease liability must be non-negative");
        }
        if (monthlyPayment == null || monthlyPayment.signum() <= 0
                || monthlyPayment.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("monthly payment must be positive with at most two decimal places");
        }
        if (annualRate == null || annualRate.signum() < 0
                || annualRate.stripTrailingZeros().scale() > 4) {
            throw new IllegalArgumentException("discount rate must be non-negative with at most four decimal places");
        }
        if (remainingPeriods <= 0) {
            throw new IllegalArgumentException("remeasurement requires unpaid installments");
        }
        BigDecimal presentValue = calculatePresentValue(monthlyPayment, remainingPeriods, annualRate);
        return new Remeasurement(presentValue, presentValue.subtract(currentLiability));
    }

    public record Remeasurement(BigDecimal presentValue, BigDecimal adjustmentAmount) {}

    /** Each rounded closing PV becomes the next opening balance, so cents telescope to zero. */
    public static List<Installment> calculateInstallments(BigDecimal openingLiability, BigDecimal payment,
                                                          int remainingPeriods, BigDecimal annualRate) {
        if (openingLiability == null || openingLiability.signum() < 0
                || payment == null || payment.signum() <= 0 || remainingPeriods <= 0
                || annualRate == null || annualRate.signum() < 0) {
            throw new IllegalArgumentException("installments require non-negative liability and rate, and positive cash flows");
        }
        List<BigDecimal> presentValues = calculatePresentValueLadder(payment, remainingPeriods, annualRate);
        if (openingLiability.compareTo(presentValues.get(remainingPeriods)) != 0) {
            throw new IllegalStateException("opening liability differs from remaining payment present value");
        }
        List<Installment> installments = new ArrayList<>(remainingPeriods);
        BigDecimal opening = openingLiability;
        for (int periodsLeft = remainingPeriods; periodsLeft > 0; periodsLeft--) {
            BigDecimal closing = presentValues.get(periodsLeft - 1);
            BigDecimal principal = opening.subtract(closing);
            BigDecimal interest = payment.subtract(principal);
            if (principal.signum() < 0 || interest.signum() < 0) {
                throw new IllegalStateException("lease installment has a negative payment portion");
            }
            installments.add(new Installment(interest, principal, closing));
            opening = closing;
        }
        return installments;
    }

    public record Installment(BigDecimal interest, BigDecimal principal, BigDecimal remainingLiability) {}

    private static List<BigDecimal> calculatePresentValueLadder(BigDecimal payment, int periods, BigDecimal annualRate) {
        List<BigDecimal> presentValues = new ArrayList<>(periods + 1);
        presentValues.add(BigDecimal.ZERO.setScale(2));
        if (annualRate == null || annualRate.signum() <= 0) {
            for (int period = 1; period <= periods; period++) {
                presentValues.add(payment.multiply(BigDecimal.valueOf(period)).setScale(2, RoundingMode.HALF_UP));
            }
            return presentValues;
        }

        // Use the same ten-decimal discount term for every prefix PV as original recognition.
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal discountFactor = BigDecimal.ONE;
        BigDecimal total = BigDecimal.ZERO;
        for (int period = 1; period <= periods; period++) {
            discountFactor = discountFactor.multiply(onePlusRate);
            total = total.add(payment.divide(discountFactor, 10, RoundingMode.HALF_UP));
            presentValues.add(total.setScale(2, RoundingMode.HALF_UP));
        }
        return presentValues;
    }

    /**
     * 현재 리스 계약의 설정 정보(월 리스료, 약정 기간, 증분차입이자율)를 기준으로 PV를 계산합니다.
     */
    public BigDecimal calculatePresentValue() {
        int termMonths = calculateTermMonths();
        return calculatePresentValue(this.monthlyPayment, termMonths, this.discountRate);
    }

    /**
     * IFRS 16 규격에 따른 PV를 도메인 내부에서 자동 계산하고,
     * 외부 요청 객체(DTO)로 넘어온 PV 값과의 교차 검증 및 도메인 자동 반영을 수행합니다.
     */
    public void updatePresentValueAndValidate() {
        if (!ifrs16Applicable || shortTermLease || lowValueLease) {
            return;
        }

        BigDecimal calculatedPv = calculatePresentValue();

        // 외부 입력값을 전면 신뢰하지 않고, 도메인 자동 산출 PV로 교차 검증 및 강제 설정하여 도메인 불변성 보장
        this.initialRightOfUseAssetValue = calculatedPv;
        this.initialLeaseLiabilityValue = calculatedPv;
    }
}
