package com.ho.account.loan.service;

import com.ho.account.loan.domain.LoanContract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EIRCalculatorTest {

    private final EIRCalculator eirCalculator = new EIRCalculator();

    @Test
    @DisplayName("수수료가 없는 경우 nominal rate와 EIR은 일치해야 함")
    void calculateEIR_WithoutFees() {
        LoanContract loan = new LoanContract();
        loan.setPrincipalAmount(new BigDecimal("12000000")); // 1200만원
        loan.setInterestRate(new BigDecimal("5.0")); // 5%
        loan.setDeferredLoanFee(BigDecimal.ZERO);
        loan.setDisbursementDate(LocalDate.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(12));

        BigDecimal eir = eirCalculator.calculateEIR(loan, 12);

        assertEquals(0, new BigDecimal("5.0000").compareTo(eir));
    }

    @Test
    @DisplayName("이연수수료(수익)가 있는 경우 EIR은 nominal rate보다 높아야 함")
    void calculateEIR_WithDeferredRevenue() {
        LoanContract loan = new LoanContract();
        loan.setPrincipalAmount(new BigDecimal("10000000")); // 1000만원
        loan.setInterestRate(new BigDecimal("6.0")); // 6%
        loan.setDeferredLoanFee(new BigDecimal("200000")); // 20만원 수수료 (금융기관 수익)
        loan.setDisbursementDate(LocalDate.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(24));

        BigDecimal eir = eirCalculator.calculateEIR(loan, 24);

        // EIR은 6.0%보다 높아야 합니다 (실제로는 약 8%대 예상)
        assertTrue(eir.compareTo(new BigDecimal("6.0")) > 0);
        System.out.println("Calculated EIR with 2% fee: " + eir + "%");
    }
}
