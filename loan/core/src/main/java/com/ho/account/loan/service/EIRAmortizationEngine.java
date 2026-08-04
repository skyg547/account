package com.ho.account.loan.service;

import com.ho.account.loan.domain.DeferredItem;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [도메인 계산기] IFRS 9 유효이자율(EIR) 및 이연대출부대손익 상각 코어 엔진.
 *
 * 💡 [초보자를 위한 개념 및 회계 수식 설명]
 * 대출 발생 시 고객에게 받은 취급수수료(부대수익)나 법무사비(부대비용)는 회계상 바로 당일 손익으로 털지 않고,
 * 유효이자율(EIR)에 반영하여 대출 기간 동안 매월 쪼개서 상각(Amortization)합니다.
 *
 * 📌 [IFRS 9 이연 상각 수식]
 * 1. 초기 장부가액 (Carrying Amount) = 원금 - 수수료수익(Inflow) + 부대비용(Outflow)
 * 2. 매월 유효이자수익 = \( \text{장부가액}_{t-1} \times \text{EIR}_{\text{monthly}} \)
 * 3. 매월 약정이자수익 = \( \text{원금잔액}_{t-1} \times \text{명목이자율}_{\text{monthly}} \)
 * 4. 매월 이연부대손익 상각액 = 유효이자수익 - 약정이자수익
 * 5. 마지막 회차는 단수 차이를 보정하여 남은 이연잔액을 0으로 맞춥니다.
 *
 * 🔧 [금융 정밀도 정책]
 * - 내부 연산 정밀도: `MathContext(34, RoundingMode.HALF_EVEN)`
 * - 금액 스케일: `setScale(2, RoundingMode.HALF_UP)`
 */
@Component
public class EIRAmortizationEngine {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    /**
     * 이연 항목 리스트로부터 최초 대출 발생 시점의 순 이연 금액(Net Deferred Amount)을 계산합니다.
     *
     * @param deferredItems 이연 항목 리스트 (수수료, 비용 등)
     * @return 순 이연 금액 (부대수익은 차감(-), 부대비용은 가산(+))
     */
    public BigDecimal calculateNetInitialDeferredAmount(List<DeferredItem> deferredItems) {
        if (deferredItems == null || deferredItems.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal netDeferred = BigDecimal.ZERO;

        for (DeferredItem item : deferredItems) {
            if (item == null || item.getStatus() == DeferredItem.DeferredItemStatus.CANCELLED) {
                continue;
            }
            if (item.getDeferredItemType() == null || item.getDeferredItemType().getEirCashFlowTreatment() == null) {
                continue;
            }

            BigDecimal amount = (item.getAmount() != null) ? item.getAmount() : BigDecimal.ZERO;

            switch (item.getDeferredItemType().getEirCashFlowTreatment()) {
                case CUSTOMER_FEE_INFLOW -> netDeferred = netDeferred.subtract(amount, MC);
                case ORIGINATION_COST_OUTFLOW -> netDeferred = netDeferred.add(amount, MC);
                case EXCLUDED_FROM_EIR -> { /* 이연 제외 */ }
            }
        }

        return netDeferred.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 회차별 상환 스케줄과 유효이자율(EIR)을 적용하여 이연 부대손익 상각 스케줄을 계산합니다.
     *
     * @param scheduleEntries     기본 원리금 상환 스케줄 항목
     * @param annualEir           연 유효이자율 (EIR, 예: 0.0520 = 5.2%)
     * @param netInitialDeferred  순 이연 금액 (부대비용-부대수익)
     * @return 회차별 이연 상각액 리스트
     */
    public List<BigDecimal> calculateMonthlyDeferredAmortizations(
            List<RepaymentScheduleEntry> scheduleEntries,
            BigDecimal annualEir,
            BigDecimal netInitialDeferred) {

        if (scheduleEntries == null || scheduleEntries.isEmpty()) {
            return List.of();
        }
        BigDecimal safeEir = (annualEir != null && annualEir.signum() >= 0) ? annualEir : BigDecimal.ZERO;
        BigDecimal safeDeferred = (netInitialDeferred != null) ? netInitialDeferred : BigDecimal.ZERO;

        int periods = scheduleEntries.size();
        List<BigDecimal> amortizations = new ArrayList<>(periods);

        if (safeDeferred.signum() == 0) {
            for (int i = 0; i < periods; i++) {
                amortizations.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
            return List.copyOf(amortizations);
        }

        BigDecimal monthlyEir = safeEir.divide(MONTHS_PER_YEAR, MC);
        BigDecimal remainingUnamortized = safeDeferred.abs(); // 상각 대상 절대 금액
        BigDecimal signFactor = (safeDeferred.signum() >= 0) ? BigDecimal.ONE : BigDecimal.valueOf(-1);

        BigDecimal regularAmortization = remainingUnamortized.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);

        for (int i = 0; i < periods; i++) {
            boolean isLastPeriod = (i == periods - 1);
            BigDecimal currentAmortization;

            if (isLastPeriod) {
                currentAmortization = remainingUnamortized;
            } else {
                // EIR 기반 상각액 계산: Effective Interest - Nominal Interest
                RepaymentScheduleEntry entry = scheduleEntries.get(i);
                BigDecimal nominalInterest = entry.interestPayment();
                
                // 장부가액 기준 유효이자 산출
                BigDecimal effectiveInterest = entry.beginningBalance().multiply(monthlyEir, MC).setScale(2, RoundingMode.HALF_UP);
                BigDecimal diffAmortization = effectiveInterest.subtract(nominalInterest, MC).abs();

                if (diffAmortization.signum() > 0) {
                    currentAmortization = diffAmortization.min(remainingUnamortized);
                } else {
                    currentAmortization = regularAmortization.min(remainingUnamortized);
                }
            }

            remainingUnamortized = remainingUnamortized.subtract(currentAmortization, MC).max(BigDecimal.ZERO);
            BigDecimal signedAmortization = currentAmortization.multiply(signFactor, MC).setScale(2, RoundingMode.HALF_UP);
            amortizations.add(signedAmortization);
        }

        return List.copyOf(amortizations);
    }

    private static final int MAX_ITERATIONS = 100;
    private static final BigDecimal CONVERGENCE_PRECISION = new BigDecimal("0.000000000000000001");

    /**
     * 초기 대출 원금, 순 이연 금액(부대비용-부대수익), 회차별 현금흐름(원리금) 리스트가 주어졌을 때
     * pure domain 내부에서 Newton-Raphson 수치해석 알고리즘으로 유효이자율(EIR, 연율)을 자동 산출합니다.
     *
     * 현금흐름 수식:
     * \( \text{순 투자액} = \text{원금} + \text{순이연금액} = \sum_{t=1}^{N} \frac{\text{CashFlow}_t}{(1 + r)^t} \)
     *
     * @param principal           대출 원금
     * @param netInitialDeferred  순 이연 금액 (부대수익은 -, 부대비용은 +)
     * @param cashFlows           회차별 현금흐름(원리금) 리스트
     * @return 수렴된 연 유효이자율 (EIR, 예: 0.0520 = 5.2%)
     */
    public BigDecimal solveEIRWithNewtonRaphson(
            BigDecimal principal,
            BigDecimal netInitialDeferred,
            List<BigDecimal> cashFlows) {

        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive.");
        }
        if (cashFlows == null || cashFlows.isEmpty()) {
            throw new IllegalArgumentException("cashFlows must not be null or empty.");
        }

        BigDecimal safeDeferred = (netInitialDeferred != null) ? netInitialDeferred : BigDecimal.ZERO;
        BigDecimal netInvestment = principal.add(safeDeferred, MC);
        if (netInvestment.signum() <= 0) {
            throw new IllegalArgumentException("netInvestment must be positive.");
        }

        int periods = cashFlows.size();
        BigDecimal monthlyRate = new BigDecimal("0.05").divide(MONTHS_PER_YEAR, MC);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate, MC);
            if (onePlusRate.signum() <= 0) {
                throw new IllegalStateException("Newton-Raphson iteration crossed rate boundary.");
            }

            BigDecimal fValue = netInvestment.negate();
            BigDecimal fDerivative = BigDecimal.ZERO;

            for (int t = 1; t <= periods; t++) {
                BigDecimal cashFlow = cashFlows.get(t - 1);
                if (cashFlow == null) cashFlow = BigDecimal.ZERO;

                BigDecimal discountFactor = onePlusRate.pow(t, MC);
                fValue = fValue.add(cashFlow.divide(discountFactor, MC), MC);

                BigDecimal derivativeTerm = cashFlow.multiply(BigDecimal.valueOf(t), MC)
                        .divide(discountFactor.multiply(onePlusRate, MC), MC);
                fDerivative = fDerivative.subtract(derivativeTerm, MC);
            }

            if (fDerivative.abs().compareTo(CONVERGENCE_PRECISION) <= 0) {
                throw new IllegalStateException("Newton-Raphson derivative is too small to converge.");
            }

            BigDecimal nextMonthlyRate = monthlyRate.subtract(fValue.divide(fDerivative, MC), MC);
            if (nextMonthlyRate.subtract(monthlyRate, MC).abs().compareTo(CONVERGENCE_PRECISION) <= 0) {
                return nextMonthlyRate.multiply(MONTHS_PER_YEAR, MC).setScale(4, RoundingMode.HALF_UP);
            }

            monthlyRate = nextMonthlyRate;
        }

        throw new IllegalStateException("Newton-Raphson solver failed to converge within " + MAX_ITERATIONS + " iterations.");
    }

    /**
     * 회차별 상환 스케줄과 순 이연 금액이 주어졌을 때,
     * 내부 Newton-Raphson 수치해석기로 연 EIR을 자동 산출하고 월별 이연 상각 스케줄을 산출합니다.
     */
    public List<BigDecimal> calculateMonthlyDeferredAmortizationsWithSolver(
            BigDecimal principal,
            List<RepaymentScheduleEntry> scheduleEntries,
            BigDecimal netInitialDeferred) {

        if (scheduleEntries == null || scheduleEntries.isEmpty()) {
            return List.of();
        }

        List<BigDecimal> cashFlows = scheduleEntries.stream()
                .map(RepaymentScheduleEntry::totalPayment)
                .toList();

        BigDecimal solvedAnnualEir = solveEIRWithNewtonRaphson(principal, netInitialDeferred, cashFlows);
        return calculateMonthlyDeferredAmortizations(scheduleEntries, solvedAnnualEir, netInitialDeferred);
    }
}
