package com.ho.account.reconciliation.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconciliationTolerancePolicyTest {

    private final ReconciliationTolerancePolicy policy = new ReconciliationTolerancePolicy();

    @Test
    void resolveAmountToleranceUsesFirstActiveRuleByPriorityOrder() {
        ReconciliationRule inactiveRule = rule(ReconciliationRule.ToleranceType.ABSOLUTE, "999.00", false);
        ReconciliationRule activeRule = rule(ReconciliationRule.ToleranceType.ABSOLUTE, "5.00", true);

        BigDecimal tolerance = policy.resolveAmountTolerance(List.of(inactiveRule, activeRule), new BigDecimal("1000.00"));

        assertThat(tolerance).isEqualByComparingTo("5.00");
    }

    @Test
    void resolveAmountToleranceCalculatesPercentageAgainstAmountBasis() {
        ReconciliationRule rule = rule(ReconciliationRule.ToleranceType.PERCENTAGE, "2.5", true);

        BigDecimal tolerance = policy.resolveAmountTolerance(List.of(rule), new BigDecimal("1000.00"));

        assertThat(tolerance).isEqualByComparingTo("25.000");
    }

    @Test
    void resolveAmountToleranceRejectsNegativeToleranceValue() {
        ReconciliationRule rule = rule(ReconciliationRule.ToleranceType.ABSOLUTE, "-0.01", true);

        assertThatThrownBy(() -> policy.resolveAmountTolerance(List.of(rule), new BigDecimal("1000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toleranceValue");
    }

    private ReconciliationRule rule(ReconciliationRule.ToleranceType toleranceType, String toleranceValue,
            boolean active) {
        ReconciliationRule rule = new ReconciliationRule();
        rule.setToleranceType(toleranceType);
        rule.setToleranceValue(new BigDecimal(toleranceValue));
        rule.setActive(active);
        return rule;
    }
}
