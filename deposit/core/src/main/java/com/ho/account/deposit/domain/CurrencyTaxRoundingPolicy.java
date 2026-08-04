package com.ho.account.deposit.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [도메인 정책 Enum] 수신 통화별 세금 원천징수 절사/반올림 정책.
 *
 * 📌 [통화별 절사 및 반올림 수칙]
 * - KRW (원화): 한국 금융세법에 따라 1원 미만 절사 (0 decimal, FLOOR)
 * - USD (달러): Cents 단위 반올림 (2 decimal, HALF_UP)
 * - EUR (유로): Cents 단위 반올림 (2 decimal, HALF_UP)
 * - JPY (엔화): 1엔 미만 절사 (0 decimal, FLOOR)
 */
public enum CurrencyTaxRoundingPolicy {
    KRW("KRW", 0, RoundingMode.FLOOR),
    USD("USD", 2, RoundingMode.HALF_UP),
    EUR("EUR", 2, RoundingMode.HALF_UP),
    JPY("JPY", 0, RoundingMode.FLOOR);

    private final String currencyCode;
    private final int scale;
    private final RoundingMode roundingMode;

    CurrencyTaxRoundingPolicy(String currencyCode, int scale, RoundingMode roundingMode) {
        this.currencyCode = currencyCode;
        this.scale = scale;
        this.roundingMode = roundingMode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public int getScale() {
        return scale;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public static CurrencyTaxRoundingPolicy of(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return KRW;
        }
        for (CurrencyTaxRoundingPolicy policy : values()) {
            if (policy.currencyCode.equalsIgnoreCase(currencyCode.trim())) {
                return policy;
            }
        }
        return KRW;
    }

    public BigDecimal applyRounding(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(scale, roundingMode).setScale(2, RoundingMode.HALF_UP);
    }
}
