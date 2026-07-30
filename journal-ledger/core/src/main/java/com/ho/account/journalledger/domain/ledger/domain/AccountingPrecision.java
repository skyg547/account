package com.ho.account.journalledger.domain.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 원장 경계에서 사용하는 금액과 환율의 정밀도 정책입니다.
 *
 * <p>초보자 설명: {@code BigDecimal}을 쓴다는 사실만으로 금액이 안전해지는 것은 아닙니다.
 * 같은 100원이라도 어떤 코드는 소수 둘째 자리, 다른 코드는 소수 여덟째 자리로 저장하면
 * 비교와 합계 결과가 달라질 수 있습니다. 그래서 전표가 원장으로 넘어오는 한 지점에서
 * DB 컬럼 계약과 같은 자릿수를 강제합니다.</p>
 *
 * <p>원장 금액은 기존 {@code DECIMAL(19,2)} 계약에 맞추고, 환율은
 * {@code DECIMAL(19,8)}에 맞춥니다. 계산 결과를 몰래 반올림하면 차대변 불일치를 숨길 수
 * 있으므로 {@link RoundingMode#UNNECESSARY}를 사용해 호출자가 명시적으로 금액을 확정하도록
 * 합니다. IFRS 9의 유효이자율 같은 고정밀 계산은 상위 도메인에서 수행하고, 이 정책은
 * 확정된 전기 금액이 원장에 기록되는 경계를 보호합니다.</p>
 */
public final class AccountingPrecision {

    public static final int LEDGER_PRECISION = 19;
    public static final int LEDGER_SCALE = 2;
    public static final int EXCHANGE_RATE_PRECISION = 19;
    public static final int EXCHANGE_RATE_SCALE = 8;

    private AccountingPrecision() {
    }

    /**
     * 음수 잔액도 허용하는 원장 금액 정규화입니다.
     */
    public static BigDecimal ledgerAmount(BigDecimal value) {
        return normalize(value, LEDGER_PRECISION, LEDGER_SCALE, "원장 금액");
    }

    /**
     * 차변·대변처럼 0 이상이어야 하는 금액을 정규화합니다.
     */
    public static BigDecimal nonNegativeLedgerAmount(BigDecimal value) {
        BigDecimal normalized = ledgerAmount(value);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException("차변/대변 금액은 음수일 수 없습니다.");
        }
        return normalized;
    }

    /**
     * 전표 라인처럼 반드시 0보다 커야 하는 금액을 정규화합니다.
     */
    public static BigDecimal positiveLedgerAmount(BigDecimal value) {
        BigDecimal normalized = nonNegativeLedgerAmount(value);
        if (normalized.signum() == 0) {
            throw new IllegalArgumentException("전표 금액은 0보다 커야 합니다.");
        }
        return normalized;
    }

    /**
     * 거래통화에서 기준통화로 변환할 때 쓰는 양수 환율을 정규화합니다.
     */
    public static BigDecimal exchangeRate(BigDecimal value) {
        BigDecimal normalized = normalize(
                value,
                EXCHANGE_RATE_PRECISION,
                EXCHANGE_RATE_SCALE,
                "환율");
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException("환율은 0보다 커야 합니다.");
        }
        return normalized;
    }

    private static BigDecimal normalize(BigDecimal value, int precision, int scale, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + "은(는) 필수입니다.");
        }

        final BigDecimal normalized;
        try {
            normalized = value.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    label + "은(는) 소수점 " + scale + "자리를 초과할 수 없습니다.", exception);
        }

        if (normalized.precision() > precision) {
            throw new IllegalArgumentException(
                    label + "은(는) 전체 " + precision + "자리를 초과할 수 없습니다.");
        }
        return normalized;
    }
}
