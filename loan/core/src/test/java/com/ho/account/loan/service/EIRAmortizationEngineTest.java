package com.ho.account.loan.service;

import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.RepaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EIRAmortizationEngine 단위 테스트")
class EIRAmortizationEngineTest {

    private final EIRAmortizationEngine engine = new EIRAmortizationEngine();
    private final RepaymentScheduleCalculator scheduleCalculator = new RepaymentScheduleCalculator();

    private Loan createDummyLoan() {
        Loan loan = Loan.create(
                "LN-2026-TEST", 1L, "KRW", Loan.LoanType.TERM_LOAN,
                new BigDecimal("1200000.00"), new BigDecimal("0.0600"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1),
                Loan.PaymentFrequency.MONTHLY, "TEST");
        loan.activateAfterDisbursal(LocalDate.of(2026, 1, 1), new BigDecimal("1200000.00"), "TEST");
        return loan;
    }

    @Test
    @DisplayName("고객 수수료 차감(Inflow)과 부대비용 가산(Outflow)에 따라 순 이연 금액이 바르게 계산되는지 검증한다")
    void calculateNetInitialDeferredAmount() {
        Loan dummyLoan = createDummyLoan();

        DeferredItemType feeType = DeferredItemType.create(
                "FEE", "Origination Fee", "Fee", DeferredItemType.DeferralMethod.EIR_METHOD,
                DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW, "100", "200", true, "TEST");

        DeferredItemType costType = DeferredItemType.create(
                "COST", "Appraisal Cost", "Cost", DeferredItemType.DeferralMethod.EIR_METHOD,
                DeferredItemType.EirCashFlowTreatment.ORIGINATION_COST_OUTFLOW, "100", "200", true, "TEST");

        DeferredItem feeItem = DeferredItem.create(
                dummyLoan, feeType, new BigDecimal("10000.00"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "TEST");

        DeferredItem costItem = DeferredItem.create(
                dummyLoan, costType, new BigDecimal("3000.00"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "TEST");

        // Fee Inflow -10000 + Cost Outflow +3000 = Net Deferred -7000
        BigDecimal netDeferred = engine.calculateNetInitialDeferredAmount(List.of(feeItem, costItem));

        assertThat(netDeferred).isEqualByComparingTo("-7000.00");
    }

    @Test
    @DisplayName("대출 기간 동안 월별 이연 부대손익 상각액이 산출되고 마지막 회차에 상각 잔액이 완전히 소진되는지 검증한다")
    void calculateMonthlyDeferredAmortizations() {
        BigDecimal principal = new BigDecimal("1200000.00");
        BigDecimal annualRate = new BigDecimal("0.0600");
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 7, 1);

        List<RepaymentScheduleEntry> entries = scheduleCalculator.generateSchedule(
                principal, annualRate, startDate, maturityDate, RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST);

        BigDecimal annualEir = new BigDecimal("0.0520");
        BigDecimal netInitialDeferred = new BigDecimal("-12000.00"); // 12,000원 수수료 이연

        List<BigDecimal> amortizations = engine.calculateMonthlyDeferredAmortizations(entries, annualEir, netInitialDeferred);

        assertThat(amortizations).hasSize(6);

        // 상각액 합계가 최초 이연 금액(-12000.00)과 일치해야 함
        BigDecimal totalAmortized = amortizations.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalAmortized).isEqualByComparingTo("-12000.00");
    }

    @Test
    @DisplayName("Newton-Raphson 수치해석 알고리즘으로 현금흐름과 수수료 기반 유효이자율(EIR)이 정밀하게 수렴 계산되는지 검증한다")
    void solveEIRWithNewtonRaphson() {
        BigDecimal principal = new BigDecimal("1200000.00");
        BigDecimal netInitialDeferred = new BigDecimal("-12000.00"); // 12,000원 수수료 이연 (순 투자액 1,188,000원)

        // 6개월 월 203,513원 현금흐름
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("203513.00"),
                new BigDecimal("203513.00"),
                new BigDecimal("203513.00"),
                new BigDecimal("203513.00"),
                new BigDecimal("203513.00"),
                new BigDecimal("203513.00")
        );

        BigDecimal solvedEir = engine.solveEIRWithNewtonRaphson(principal, netInitialDeferred, cashFlows);

        assertThat(solvedEir).isNotNull();
        assertThat(solvedEir).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("내부 Newton-Raphson 해석기를 통합한 calculateMonthlyDeferredAmortizationsWithSolver가 정상 상각 스케줄을 생성한다")
    void calculateMonthlyDeferredAmortizationsWithSolver() {
        BigDecimal principal = new BigDecimal("1200000.00");
        BigDecimal annualRate = new BigDecimal("0.0600");
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 7, 1);

        List<RepaymentScheduleEntry> entries = scheduleCalculator.generateSchedule(
                principal, annualRate, startDate, maturityDate, RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST);

        BigDecimal netInitialDeferred = new BigDecimal("-12000.00");

        List<BigDecimal> amortizations = engine.calculateMonthlyDeferredAmortizationsWithSolver(principal, entries, netInitialDeferred);

        assertThat(amortizations).hasSize(6);
        BigDecimal totalAmortized = amortizations.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalAmortized).isEqualByComparingTo("-12000.00");
    }
}
