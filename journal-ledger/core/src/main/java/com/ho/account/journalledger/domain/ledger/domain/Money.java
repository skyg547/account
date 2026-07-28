package com.ho.account.journalledger.domain.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 금액을 표현하는 불변 Value Object.
 *
 * <p>초보자 설명: 회계 금액은 부동소수점 오차를 피하기 위해 BigDecimal로 다루고,
 * 생성 시점에 소수점 2자리로 맞춰 도메인 전체의 금액 표현 규칙을 통일한다.
 */
public record Money(BigDecimal amount) {
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        // 소수점 2자리 반올림. 통화 금액 표시/저장 기준을 한 곳에서 맞춘다.
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
