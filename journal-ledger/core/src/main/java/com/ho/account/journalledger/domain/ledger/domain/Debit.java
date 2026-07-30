package com.ho.account.journalledger.domain.ledger.domain;

import java.math.BigDecimal;

/**
 * 차변 금액을 나타내는 불변 Value Object입니다.
 *
 * <p>단순한 {@code BigDecimal} 대신 타입을 분리하면 메서드 인자 순서를 바꾸어 차변을
 * 대변 칸에 넣는 실수를 컴파일 단계와 도메인 검증에서 더 쉽게 발견할 수 있습니다.</p>
 */
public record Debit(BigDecimal amount) {

    public static final Debit ZERO = new Debit(BigDecimal.ZERO);

    public Debit {
        amount = AccountingPrecision.nonNegativeLedgerAmount(amount);
    }

    public static Debit of(BigDecimal amount) {
        return new Debit(amount);
    }

    public Debit add(Debit other) {
        if (other == null) {
            throw new IllegalArgumentException("더할 차변 금액은 필수입니다.");
        }
        return new Debit(amount.add(other.amount));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }
}
