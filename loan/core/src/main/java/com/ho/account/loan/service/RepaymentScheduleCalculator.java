package com.ho.account.loan.service;

import com.ho.account.loan.domain.RepaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [도메인 계산기] 대출 상환 방식별 원리금 상환 스케줄 생성기.
 *
 * 💡 [초보자를 위한 수학 및 업무 개념 설명]
 * 이 클래스는 대출 원금, 명목 이자율, 만기 및 상환 방식에 따라 회차별 원금과 이자를 정밀 산출합니다.
 *
 * 📌 [상환 방식별 수학 수식]
 * 1. EQUAL_PRINCIPAL_AND_INTEREST (원리금 균등상환):
 *    매월 총 상환액 \( PMT = P \times \frac{r(1+r)^n}{(1+r)^n - 1} \)
 *    - 원금 상환액 = PMT - (기초잔액 × 월이자율)
 *    - 마지막 회차는 원금 잔액 전액을 정산하여 단수차이를 보정합니다.
 *
 * 2. EQUAL_PRINCIPAL (원금 균등상환):
 *    매월 정기 원금 상환액 = \( P / n \)
 *    - 이자 = 기초잔액 × 월이자율
 *    - 총 상환액 = 매월 정기 원금 + 당월 이자
 *
 * 3. BULLET_MATURITY (만기 일시상환):
 *    - 1회차 ~ (n-1)회차: 원금 상환액 = 0, 이자 = 기초잔액 × 월이자율
 *    - n회차 (만기일): 원금 상환액 = P 전액, 이자 = 기초잔액 × 월이자율
 *
 * 🔧 [정밀도 정책]
 * - 내부 연산: `MathContext(34, RoundingMode.HALF_EVEN)`
 * - 최종 원화 금액: `setScale(2, RoundingMode.HALF_UP)`
 */
@Component
public class RepaymentScheduleCalculator {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    /**
     * 지정된 대출 조건 및 상환 방식에 맞춰 전 기간 상환 스케줄 리스트를 생성합니다.
     *
     * @param principal       대출 실행 원금 (P)
     * @param annualRate      명목 연 이자율 (r, 예: 0.05 = 5.0%)
     * @param startDate       대출 실행일
     * @param maturityDate    대출 만기일
     * @param method          상환 방식 (원리금균등, 원금균등, 만기일시)
     * @return 각 회차별 불변 상환 스케줄 항목 리스트
     */
    public List<RepaymentScheduleEntry> generateSchedule(
            BigDecimal principal,
            BigDecimal annualRate,
            LocalDate startDate,
            LocalDate maturityDate,
            RepaymentMethod method) {

        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive.");
        }
        if (annualRate == null || annualRate.signum() < 0 || annualRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("annualRate must be a decimal rate between 0 and 1.");
        }
        if (startDate == null || maturityDate == null || !startDate.isBefore(maturityDate)) {
            throw new IllegalArgumentException("startDate must be before maturityDate.");
        }
        RepaymentMethod safeMethod = (method != null) ? method : RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST;

        List<LocalDate> paymentDates = calculatePaymentDates(startDate, maturityDate);
        int periods = paymentDates.size();

        BigDecimal monthlyRate = annualRate.divide(MONTHS_PER_YEAR, MC);

        return switch (safeMethod) {
            case EQUAL_PRINCIPAL_AND_INTEREST -> generateEqualPaymentSchedule(principal, monthlyRate, paymentDates);
            case EQUAL_PRINCIPAL -> generateEqualPrincipalSchedule(principal, monthlyRate, paymentDates);
            case BULLET_MATURITY -> generateBulletMaturitySchedule(principal, monthlyRate, paymentDates);
        };
    }

    /**
     * 원리금 균등상환 스케줄 생성
     */
    private List<RepaymentScheduleEntry> generateEqualPaymentSchedule(
            BigDecimal principal, BigDecimal monthlyRate, List<LocalDate> paymentDates) {
        int periods = paymentDates.size();
        BigDecimal pmt = calculatePMT(principal, monthlyRate, periods);

        List<RepaymentScheduleEntry> entries = new ArrayList<>(periods);
        BigDecimal currentBalance = principal;

        for (int i = 0; i < periods; i++) {
            boolean isLastPeriod = (i == periods - 1);
            BigDecimal interestPayment = currentBalance.multiply(monthlyRate, MC).setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalPayment;
            if (isLastPeriod) {
                principalPayment = currentBalance;
            } else {
                principalPayment = pmt.subtract(interestPayment, MC).min(currentBalance).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal totalPayment = principalPayment.add(interestPayment, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal endingBalance = currentBalance.subtract(principalPayment, MC).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            entries.add(new RepaymentScheduleEntry(
                    i + 1,
                    paymentDates.get(i),
                    currentBalance.setScale(2, RoundingMode.HALF_UP),
                    principalPayment,
                    interestPayment,
                    totalPayment,
                    endingBalance
            ));

            currentBalance = endingBalance;
        }

        return List.copyOf(entries);
    }

    /**
     * 원금 균등상환 스케줄 생성
     */
    private List<RepaymentScheduleEntry> generateEqualPrincipalSchedule(
            BigDecimal principal, BigDecimal monthlyRate, List<LocalDate> paymentDates) {
        int periods = paymentDates.size();
        BigDecimal regularPrincipal = principal.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);

        List<RepaymentScheduleEntry> entries = new ArrayList<>(periods);
        BigDecimal currentBalance = principal;

        for (int i = 0; i < periods; i++) {
            boolean isLastPeriod = (i == periods - 1);
            BigDecimal interestPayment = currentBalance.multiply(monthlyRate, MC).setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalPayment = isLastPeriod ? currentBalance : regularPrincipal.min(currentBalance);
            BigDecimal totalPayment = principalPayment.add(interestPayment, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal endingBalance = currentBalance.subtract(principalPayment, MC).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            entries.add(new RepaymentScheduleEntry(
                    i + 1,
                    paymentDates.get(i),
                    currentBalance.setScale(2, RoundingMode.HALF_UP),
                    principalPayment,
                    interestPayment,
                    totalPayment,
                    endingBalance
            ));

            currentBalance = endingBalance;
        }

        return List.copyOf(entries);
    }

    /**
     * 만기 일시상환 스케줄 생성
     */
    private List<RepaymentScheduleEntry> generateBulletMaturitySchedule(
            BigDecimal principal, BigDecimal monthlyRate, List<LocalDate> paymentDates) {
        int periods = paymentDates.size();
        List<RepaymentScheduleEntry> entries = new ArrayList<>(periods);
        BigDecimal currentBalance = principal;

        for (int i = 0; i < periods; i++) {
            boolean isLastPeriod = (i == periods - 1);
            BigDecimal interestPayment = currentBalance.multiply(monthlyRate, MC).setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalPayment = isLastPeriod ? currentBalance : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPayment = principalPayment.add(interestPayment, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal endingBalance = currentBalance.subtract(principalPayment, MC).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            entries.add(new RepaymentScheduleEntry(
                    i + 1,
                    paymentDates.get(i),
                    currentBalance.setScale(2, RoundingMode.HALF_UP),
                    principalPayment,
                    interestPayment,
                    totalPayment,
                    endingBalance
            ));

            currentBalance = endingBalance;
        }

        return List.copyOf(entries);
    }

    /**
     * 원리금 균등상환월액 PMT 계산 수식:
     * \( PMT = P \times \frac{r(1+r)^n}{(1+r)^n - 1} \)
     */
    private BigDecimal calculatePMT(BigDecimal principal, BigDecimal monthlyRate, int periods) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
        }
        BigDecimal compound = BigDecimal.ONE.add(monthlyRate, MC).pow(periods, MC);
        BigDecimal num = principal.multiply(monthlyRate, MC).multiply(compound, MC);
        BigDecimal den = compound.subtract(BigDecimal.ONE, MC);
        return num.divide(den, MC).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 시작일부터 만기일까지 월단위 상환일자 목록 산출
     */
    private List<LocalDate> calculatePaymentDates(LocalDate startDate, LocalDate maturityDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = startDate.plusMonths(1);
        while (cursor.isBefore(maturityDate)) {
            dates.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        dates.add(maturityDate);
        return dates;
    }
}
