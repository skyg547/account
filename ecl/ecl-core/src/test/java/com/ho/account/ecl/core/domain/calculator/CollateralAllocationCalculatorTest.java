package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.ecl.core.domain.calculator.CollateralAllocationCalculator.AllocationResult;
import com.ho.account.ecl.core.domain.collateral.CrCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CollateralAllocationCalculatorTest {

    private CollateralAllocationCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CollateralAllocationCalculator();
    }

    @Test
    @DisplayName("LP 최적화 계산 시 손실 절감 우선순위가 높은 계좌에 담보가 우선 배정된다.")
    void calculateLpOptimization_PrioritizesHighPriorityAccount() {
        CrAccount accCorp = CrAccount.builder()
                .id(1L)
                .accountNo("ACC-CORP")
                .productCode("CORP")
                .outstandingAmount(new BigDecimal("100"))
                .build();

        CrAccount accMortgage = CrAccount.builder()
                .id(2L)
                .accountNo("ACC-MORTGAGE")
                .productCode("MORTGAGE")
                .outstandingAmount(new BigDecimal("100"))
                .build();

        CrCollateral collateral = CrCollateral.builder()
                .id(10L)
                .collateralCode("COLL-01")
                .collateralType("CASH")
                .appraisalAmount(new BigDecimal("100"))
                .baseHaircut(BigDecimal.ZERO)
                .build();

        List<AllocationResult> results = calculator.calculateLpOptimization(
                List.of(accCorp, accMortgage),
                List.of(collateral)
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAccount().getAccountNo()).isEqualTo("ACC-CORP");
        assertThat(results.get(0).getAllocatedAmount()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    @DisplayName("폭포수(Waterfall) 배정 계산 시 우선순위 높은 계좌에 순차 배정된다.")
    void calculateWaterfallAllocation_Success() {
        CrAccount accCorp = CrAccount.builder()
                .id(1L)
                .accountNo("ACC-CORP")
                .productCode("CORP")
                .outstandingAmount(new BigDecimal("100"))
                .build();

        CrCollateral collateral = CrCollateral.builder()
                .id(10L)
                .collateralCode("COLL-01")
                .collateralType("CASH")
                .appraisalAmount(new BigDecimal("50"))
                .baseHaircut(BigDecimal.ZERO)
                .build();

        List<AllocationResult> results = calculator.calculateWaterfallAllocation(
                List.of(accCorp),
                List.of(collateral),
                null
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAllocatedAmount()).isEqualByComparingTo(new BigDecimal("50"));
    }
}
