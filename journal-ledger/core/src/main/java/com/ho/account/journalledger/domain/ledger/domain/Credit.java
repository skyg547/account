package com.ho.account.journalledger.domain.ledger.domain;

import java.math.BigDecimal;

/**
 * 대변 금액을 나타내는 불변 Value Object입니다.
 *
 * <p>{@link Debit}과 별도 타입으로 두는 이유는 금액의 숫자값뿐 아니라 회계적 방향도
 * 도메인 의미이기 때문입니다. 두 타입 모두 같은 정밀도 정책을 공유하지만 서로 바꿔
 * 전달할 수는 없습니다.</p>
 */
public record Credit(BigDecimal amount) {

    public static final Credit ZERO = new Credit(BigDecimal.ZERO);

    public Credit {
        amount = AccountingPrecision.nonNegativeLedgerAmount(amount);
    }

    public static Credit of(BigDecimal amount) {
        return new Credit(amount);
    }

    public Credit add(Credit other) {
        if (other == null) {
            throw new IllegalArgumentException("더할 대변 금액은 필수입니다.");
        }
        return new Credit(amount.add(other.amount));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }
}
