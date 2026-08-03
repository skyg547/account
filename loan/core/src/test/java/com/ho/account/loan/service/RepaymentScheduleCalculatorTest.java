package com.ho.account.loan.service;

import com.ho.account.loan.domain.RepaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RepaymentScheduleCalculator 단위 테스트")
class RepaymentScheduleCalculatorTest {

    private final RepaymentScheduleCalculator calculator = new RepaymentScheduleCalculator();

    @Test
    @DisplayName("원리금 균등상환 스케줄이 바르게 생성되고 기말 잔액이 0이 되는지 검증한다")
    void generateSchedule_EqualPrincipalAndInterest() {
        BigDecimal principal = new BigDecimal("1200000.00");
        BigDecimal annualRate = new BigDecimal("0.0600"); // 6.0%
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 7, 1); // 6개월 대출

        List<RepaymentScheduleEntry> schedule = calculator.generateSchedule(
                principal, annualRate, startDate, maturityDate, RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST);

        assertThat(schedule).hasSize(6);

        // 1회차 검증: 기초잔액 1,200,000
        RepaymentScheduleEntry first = schedule.get(0);
        assertThat(first.beginningBalance()).isEqualByComparingTo("1200000.00");
        // 약정이자: 1,200,000 * 0.06 / 12 = 6,000
        assertThat(first.interestPayment()).isEqualByComparingTo("6000.00");

        // 마지막 회차 검증: 기말잔액 0.00
        RepaymentScheduleEntry last = schedule.get(5);
        assertThat(last.endingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("원금 균등상환 스케줄이 매월 일정 원금을 상환하는지 검증한다")
    void generateSchedule_EqualPrincipal() {
        BigDecimal principal = new BigDecimal("1200000.00");
        BigDecimal annualRate = new BigDecimal("0.0600"); // 6.0%
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 7, 1); // 6개월 대출 (월 200,000원 원금 상환)

        List<RepaymentScheduleEntry> schedule = calculator.generateSchedule(
                principal, annualRate, startDate, maturityDate, RepaymentMethod.EQUAL_PRINCIPAL);

        assertThat(schedule).hasSize(6);

        // 매월 정기 원금 상환액 200,000원
        for (int i = 0; i < 5; i++) {
            assertThat(schedule.get(i).principalPayment()).isEqualByComparingTo("200000.00");
        }

        // 마지막 회차 기말잔액 0.00
        assertThat(schedule.get(5).endingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("만기 일시상환 스케줄이 기간 중 이자만 납부하다가 만기일에 원금 전액을 상환하는지 검증한다")
    void generateSchedule_BulletMaturity() {
        BigDecimal principal = new BigDecimal("1000000.00");
        BigDecimal annualRate = new BigDecimal("0.0500"); // 5.0%
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 5, 1); // 4개월 대출

        List<RepaymentScheduleEntry> schedule = calculator.generateSchedule(
                principal, annualRate, startDate, maturityDate, RepaymentMethod.BULLET_MATURITY);

        assertThat(schedule).hasSize(4);

        // 1~3회차: 원금상환액 0, 이자만 납부
        for (int i = 0; i < 3; i++) {
            assertThat(schedule.get(i).principalPayment()).isEqualByComparingTo("0.00");
            assertThat(schedule.get(i).endingBalance()).isEqualByComparingTo("1000000.00");
        }

        // 4회차(만기): 원금상환액 1,000,000원, 기말잔액 0.00
        assertThat(schedule.get(3).principalPayment()).isEqualByComparingTo("1000000.00");
        assertThat(schedule.get(3).endingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("유효하지 않은 입력값(음수 원금, 만기일 오류 등)에 대해 예외가 발생한다")
    void generateSchedule_InvalidInputs() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 7, 1);

        assertThatThrownBy(() -> calculator.generateSchedule(
                new BigDecimal("-100"), new BigDecimal("0.05"), startDate, maturityDate, RepaymentMethod.EQUAL_PRINCIPAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("principal must be positive");

        assertThatThrownBy(() -> calculator.generateSchedule(
                new BigDecimal("1000"), new BigDecimal("0.05"), maturityDate, startDate, RepaymentMethod.EQUAL_PRINCIPAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate must be before maturityDate");
    }
}
