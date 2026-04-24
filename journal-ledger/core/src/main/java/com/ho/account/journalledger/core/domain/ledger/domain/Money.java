package com.ho.account.journalledger.core.domain.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 금액을 표현하는 불변 Value Object
 */
public record Money(BigDecimal amount) {
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        // 소수점 2자리 반올림
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
