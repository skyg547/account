package com.ho.account.loan.service;

import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.Loan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 대출 유효이자율(EIR) 계산기
 * Newton-Raphson 방식을 사용하여 내부수익률(IRR)을 계산합니다.
 */
@Component
public class EIRCalculator {

    private static final int MAX_ITERATIONS = 100;
    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final BigDecimal PRECISION = new BigDecimal("0.000000000000000001");
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    /**
     * 대출 계약의 EIR을 계산합니다.
     * 
     * @param loan 대출 정보 (원금, 명목이자율 정보 포함)
     * @param deferredItems 이연 항목 목록 (수수료, 비용 등)
     * @return 계산된 유효이자율(연율 소수 비율, 예: 12%는 0.1200)
     */
    public BigDecimal calculateEIR(Loan loan, List<DeferredItem> deferredItems) {
        if (loan == null) {
            throw new IllegalArgumentException("loan is required.");
        }
        return calculateEIR(
                loan,
                loan.getOutstandingPrincipal(),
                loan.getMaturityDate(),
                deferredItems);
    }

    public BigDecimal calculateEIR(
            Loan loan,
            BigDecimal proposedOutstandingPrincipal,
            LocalDate proposedMaturityDate,
            List<DeferredItem> deferredItems) {
        if (loan == null) {
            throw new IllegalArgumentException("loan is required.");
        }
        List<DeferredItem> items = deferredItems == null ? List.of() : List.copyOf(deferredItems);
        BigDecimal principal = requirePositive(proposedOutstandingPrincipal, "outstandingPrincipal");
        BigDecimal nominalAnnualRate = requireRate(loan.getInterestRate());
        if (loan.getDisbursalDate() == null || proposedMaturityDate == null
                || !proposedMaturityDate.isAfter(loan.getDisbursalDate())) {
            throw new IllegalArgumentException("A positive loan term is required for EIR calculation.");
        }
        
        // 이연 항목은 유형별 EIR 현금흐름 정책에 따라 순투자액을 늘리거나 줄입니다.
        BigDecimal initialInvestmentAdjustment = items.stream()
                .map(this::resolveInitialInvestmentAdjustment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 순 현금 유출액(투자액) = 원금 + 고객수수료/직접비용 정책별 조정액
        BigDecimal initialInvestment = principal.add(initialInvestmentAdjustment, MC);
        requirePositive(initialInvestment, "initialInvestment");

        // 총 회차 (개월 수)
        int totalPeriods = monthlyPeriods(loan.getDisbursalDate(), proposedMaturityDate);

        // 원리금 균등 상환액 (PMT) 계산 (명목 이자율 기반)
        BigDecimal nominalMonthlyRate = nominalAnnualRate.divide(MONTHS_PER_YEAR, MC);
        BigDecimal payment = calculatePayment(principal, nominalMonthlyRate, totalPeriods);

        // IRR 계산 (월 이자율)
        BigDecimal rate = nominalMonthlyRate;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            BigDecimal onePlusRate = BigDecimal.ONE.add(rate, MC);
            if (onePlusRate.signum() <= 0) {
                throw new IllegalStateException("EIR iteration crossed the -100% monthly-rate boundary.");
            }
            BigDecimal value = initialInvestment.negate();
            BigDecimal derivative = BigDecimal.ZERO;

            // f(r) = -I + sum(PMT / (1+r)^t)
            // df(r) = sum(-t * PMT / (1+r)^(t+1))
            for (int t = 1; t <= totalPeriods; t++) {
                BigDecimal discountFactor = onePlusRate.pow(t, MC);
                value = value.add(payment.divide(discountFactor, MC), MC);
                BigDecimal derivativeTerm = payment
                        .multiply(BigDecimal.valueOf(t), MC)
                        .divide(discountFactor.multiply(onePlusRate, MC), MC);
                derivative = derivative.subtract(derivativeTerm, MC);
            }

            if (derivative.abs().compareTo(PRECISION) <= 0) {
                throw new IllegalStateException("EIR derivative is too small to converge safely.");
            }
            BigDecimal nextRate = rate.subtract(value.divide(derivative, MC), MC);
            if (nextRate.subtract(rate, MC).abs().compareTo(PRECISION) <= 0) {
                return nextRate.multiply(MONTHS_PER_YEAR, MC).setScale(4, RoundingMode.HALF_UP);
            }
            rate = nextRate;
        }

        throw new IllegalStateException("EIR did not converge within " + MAX_ITERATIONS + " iterations.");
    }

    private BigDecimal resolveInitialInvestmentAdjustment(DeferredItem item) {
        if (item == null) {
            throw new IllegalArgumentException("deferredItems must not contain null.");
        }
        BigDecimal amount = requirePositive(item.getAmount(), "deferredItem.amount");
        DeferredItem.DeferredItemStatus status = item.getStatus();
        if (status == DeferredItem.DeferredItemStatus.CANCELLED) {
            return BigDecimal.ZERO;
        }
        DeferredItemTypePolicy policy = DeferredItemTypePolicy.from(item);
        return switch (policy.treatment()) {
            case CUSTOMER_FEE_INFLOW -> amount.negate();
            case ORIGINATION_COST_OUTFLOW -> amount;
            case EXCLUDED_FROM_EIR -> BigDecimal.ZERO;
        };
    }

    private int monthlyPeriods(LocalDate disbursalDate, LocalDate maturityDate) {
        long periods = ChronoUnit.MONTHS.between(disbursalDate, maturityDate);
        if (disbursalDate.plusMonths(periods).isBefore(maturityDate)) {
            periods++;
        }
        if (periods < 1 || periods > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Loan term must contain at least one supported monthly period.");
        }
        return (int) periods;
    }

    private BigDecimal calculatePayment(BigDecimal principal, BigDecimal monthlyRate, int periods) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(periods), MC);
        }
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate, MC).pow(periods, MC);
        return principal.multiply(monthlyRate, MC)
                .multiply(factor, MC)
                .divide(factor.subtract(BigDecimal.ONE, MC), MC);
    }

    private BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive.");
        }
        return value;
    }

    private BigDecimal requireRate(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("interestRate must be a decimal rate between 0 and 1.");
        }
        return value;
    }

    private record DeferredItemTypePolicy(DeferredItemType.EirCashFlowTreatment treatment) {
        private static DeferredItemTypePolicy from(DeferredItem item) {
            if (item.getDeferredItemType() == null || item.getDeferredItemType().getEirCashFlowTreatment() == null) {
                throw new IllegalArgumentException("Deferred item EIR cash-flow treatment is required.");
            }
            return new DeferredItemTypePolicy(item.getDeferredItemType().getEirCashFlowTreatment());
        }
    }
}
