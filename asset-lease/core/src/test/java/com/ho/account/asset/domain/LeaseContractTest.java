package com.ho.account.asset.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LeaseContractTest {

    @Test
    @DisplayName("할인율이 0%인 경우 미래 현금흐름 현재가치(PV)는 단순 리스료 합계와 같다")
    void calculatePresentValueWithZeroDiscountRate() {
        BigDecimal monthlyPayment = new BigDecimal("1000000");
        int termMonths = 12;
        BigDecimal annualRate = BigDecimal.ZERO;

        BigDecimal pv = LeaseContract.calculatePresentValue(monthlyPayment, termMonths, annualRate);

        assertEquals(new BigDecimal("12000000.00"), pv);
    }

    @Test
    @DisplayName("할인율 6% 적용 시 미래 리스료 현재가치(PV)가 정확히 할인되어 계산된다")
    void calculatePresentValueWithDiscountRate() {
        BigDecimal monthlyPayment = new BigDecimal("1000000");
        int termMonths = 12;
        BigDecimal annualRate = new BigDecimal("6.0");

        BigDecimal pv = LeaseContract.calculatePresentValue(monthlyPayment, termMonths, annualRate);

        // 연 6% (월 0.5%) 12개월 100만원 현금흐름의 PV는 12,000,000보다 작고 11,600,000원 대 형성
        assertTrue(pv.compareTo(new BigDecimal("12000000.00")) < 0);
        assertTrue(pv.compareTo(new BigDecimal("11000000.00")) > 0);
        assertEquals(new BigDecimal("11618932.07"), pv);
    }

    @Test
    @DisplayName("계약 시작일과 종료일 기준 총 리스 기간 개월 수가 정확히 계산된다")
    void calculateTermMonths() {
        LeaseContract contract = new LeaseContract();
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2026, 12, 31));

        int months = contract.calculateTermMonths();

        assertEquals(12, months);
    }

    @Test
    @DisplayName("외부에서 엉뚱한 PV 입력값이 들어와도 도메인 자동 계산 PV로 교차 검증 및 정정된다")
    void updatePresentValueAndValidateOverridesExternalValue() {
        LeaseContract contract = new LeaseContract();
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2026, 12, 31));
        contract.setMonthlyPayment(new BigDecimal("1000000"));
        contract.setDiscountRate(new BigDecimal("6.0"));
        contract.setIfrs16Applicable(true);

        // 외부에서 오염되거나 변조된 PV 전달
        contract.setInitialRightOfUseAssetValue(new BigDecimal("99999999.00"));
        contract.setInitialLeaseLiabilityValue(new BigDecimal("99999999.00"));

        contract.updatePresentValueAndValidate();

        // 도메인 내부 자동 계산값인 11,618,932.07 로 정정되어야 함
        assertEquals(new BigDecimal("11618932.07"), contract.getInitialRightOfUseAssetValue());
        assertEquals(new BigDecimal("11618932.07"), contract.getInitialLeaseLiabilityValue());
    }
}
