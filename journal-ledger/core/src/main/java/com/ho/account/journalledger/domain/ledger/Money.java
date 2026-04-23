package com.ho.account.journalledger.domain.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ê¸ˆì•¡???œí˜„?˜ëŠ” ë¶ˆë? Value Object (Functional Programming)
 */
public record Money(BigDecimal amount) {
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        // ?€???…ë¬´ ?œì?: ?Œìˆ˜??2?ë¦¬ ë°˜ì˜¬ë¦?        amount = amount.setScale(2, RoundingMode.HALF_UP);
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
