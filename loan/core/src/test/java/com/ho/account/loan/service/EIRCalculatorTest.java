package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EIRCalculatorTest {

    private final EIRCalculator calculator = new EIRCalculator();

    @Test
    void noDeferredItemKeepsAnnualDecimalRateUnit() {
        assertThat(calculator.calculateEIR(loan(), List.of()))
                .isEqualByComparingTo("0.1200");
    }

    @Test
    void customerFeeInflowIncreasesEirComparedWithNoDeferredItem() {
        BigDecimal baseEir = calculator.calculateEIR(loan(), List.of());
        BigDecimal feeEir = calculator.calculateEIR(
                loan(),
                List.of(deferredItem("100.00", DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW)));

        assertThat(feeEir).isGreaterThan(baseEir).isLessThan(BigDecimal.ONE);
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

    @Test
    void missingCashFlowTreatmentFailsClosed() {
        DeferredItem item = new DeferredItem();
        item.setAmount(new BigDecimal("100.00"));
        item.setStatus(DeferredItem.DeferredItemStatus.DEFERRED);

        assertThatThrownBy(() -> calculator.calculateEIR(loan(), List.of(item)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cash-flow treatment");
    }

    private Loan loan() {
        return Loan.create(
                "LN-EIR-1",
                100L,
                "KRW",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("10000.00"),
                new BigDecimal("0.1200"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                Loan.PaymentFrequency.MONTHLY,
                "tester");
    }

    private DeferredItem deferredItem(String amount, DeferredItemType.EirCashFlowTreatment treatment) {
        DeferredItemType type = DeferredItemType.create(
                "EIR-" + treatment.name(),
                treatment.name(),
                null,
                DeferredItemType.DeferralMethod.EIR_METHOD,
                treatment,
                "118000",
                "410000",
                true,
                "tester");
        DeferredItem item = new DeferredItem();
        item.setDeferredItemType(type);
        item.setAmount(new BigDecimal(amount));
        item.setStatus(DeferredItem.DeferredItemStatus.DEFERRED);
        return item;
    }
}
