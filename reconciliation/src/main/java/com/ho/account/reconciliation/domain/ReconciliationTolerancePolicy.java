package com.ho.account.reconciliation.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ReconciliationTolerancePolicy {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public BigDecimal resolveAmountTolerance(List<ReconciliationRule> rules, BigDecimal amountBasis) {
        if (rules == null || rules.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal safeAmountBasis = amountBasis == null ? BigDecimal.ZERO : amountBasis.abs();
        return rules.stream()
                .filter(Objects::nonNull)
                .filter(ReconciliationRule::isActive)
                .filter(rule -> rule.getToleranceType() != null)
                .findFirst()
                .map(rule -> resolveAmountTolerance(rule, safeAmountBasis))
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal resolveAmountTolerance(ReconciliationRule rule, BigDecimal amountBasis) {
        if (rule == null || rule.getToleranceType() == null
                || rule.getToleranceType() == ReconciliationRule.ToleranceType.NONE) {
            return BigDecimal.ZERO;
        }
        if (rule.getToleranceValue() == null) {
            return BigDecimal.ZERO;
        }
        if (rule.getToleranceValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Reconciliation toleranceValue must not be negative.");
        }

        BigDecimal safeAmountBasis = amountBasis == null ? BigDecimal.ZERO : amountBasis.abs();
        return switch (rule.getToleranceType()) {
            case ABSOLUTE -> rule.getToleranceValue();
            case PERCENTAGE -> safeAmountBasis.multiply(rule.getToleranceValue()).divide(ONE_HUNDRED);
            case NONE -> BigDecimal.ZERO;
        };
    }
}
