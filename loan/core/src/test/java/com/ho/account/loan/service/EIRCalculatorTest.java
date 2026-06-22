package com.ho.account.loan.service;

import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EIRCalculatorTest {

    private final EIRCalculator calculator = new EIRCalculator();

    @Test
    void customerFeeInflowIncreasesEirComparedWithNoDeferredItem() {
        BigDecimal baseEir = calculator.calculateEIR(loan(), List.of());

        BigDecimal feeEir = calculator.calculateEIR(
                loan(),
                List.of(deferredItem("100.00", DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW)));

        assertThat(feeEir).isGreaterThan(baseEir);
    }

    @Test
    void originationCostOutflowDecreasesEirComparedWithNoDeferredItem() {
        BigDecimal baseEir = calculator.calculateEIR(loan(), List.of());

        BigDecimal costEir = calculator.calculateEIR(
                loan(),
                List.of(deferredItem("100.00", DeferredItemType.EirCashFlowTreatment.ORIGINATION_COST_OUTFLOW)));

        assertThat(costEir).isLessThan(baseEir);
    }

    @Test
    void excludedItemDoesNotChangeEir() {
        BigDecimal baseEir = calculator.calculateEIR(loan(), List.of());

        BigDecimal excludedEir = calculator.calculateEIR(
                loan(),
                List.of(deferredItem("100.00", DeferredItemType.EirCashFlowTreatment.EXCLUDED_FROM_EIR)));

        assertThat(excludedEir).isEqualByComparingTo(baseEir);
    }

    private Loan loan() {
        Loan loan = new Loan();
        loan.setLoanNumber("LN-EIR-1");
        loan.setPrincipalAmount(new BigDecimal("10000.00"));
        loan.setInterestRate(new BigDecimal("0.1200"));
        loan.setDisbursalDate(LocalDate.of(2026, 1, 1));
        loan.setMaturityDate(LocalDate.of(2027, 1, 1));
        return loan;
    }

    private DeferredItem deferredItem(String amount, DeferredItemType.EirCashFlowTreatment treatment) {
        DeferredItemType type = new DeferredItemType();
        type.setCode("EIR-" + treatment.name());
        type.setName(treatment.name());
        type.setDeferralMethod(DeferredItemType.DeferralMethod.EIR_METHOD);
        type.setEirCashFlowTreatment(treatment);

        DeferredItem item = new DeferredItem();
        item.setDeferredItemType(type);
        item.setAmount(new BigDecimal(amount));
        item.setStatus(DeferredItem.DeferredItemStatus.DEFERRED);
        return item;
    }
}
