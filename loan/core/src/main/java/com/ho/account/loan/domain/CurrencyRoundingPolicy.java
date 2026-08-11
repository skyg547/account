package com.ho.account.loan.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [도메인 정책 Enum] 대출 통화별 금액 절사 및 반올림 정책 (Currency Rounding Policy).
 *
 * 💡 [도입 배경 및 금융 회계 수식 원칙]
 * 다중 통화(Multi-currency) 환경에서 대출 원리금 계산, 이연 상각, 일일 이자 발생 및 회계 전표(Journal Entry) 전송 시
 * 해당 통화의 법정/관례적 소수점 자리수(Scale)와 절사/반올림 규칙을 준수하지 않으면
 * 1) 원화(KRW) 전표에 소수점 단수(예: 100.55원)가 게시되어 원화 대장과 시계열 전표 불일치가 발생하고,
 * 2) 달러(USD), 유로(EUR) 등 센트(Cent) 단위 통화에서 소수점 이하 자리수가 잘못 처리되어 전표 차대변 균형 오차가 발생할 수 있습니다.
 *
 * 📌 [통화별 반올림/절사 규격]
 * 1. KRW (원화): 한국 금융세법 및 통화 규격에 따라 1원 미만 절사 (Scale: 0, RoundingMode.FLOOR)
 * 2. JPY (엔화): 일본 금융 규격에 따라 1엔 미만 절사 (Scale: 0, RoundingMode.FLOOR)
 * 3. USD (달러): Cent 단위 반올림 (Scale: 2, RoundingMode.HALF_UP)
 * 4. EUR (유로): Cent 단위 반올림 (Scale: 2, RoundingMode.HALF_UP)
 * 5. GBP (파운드): Penny 단위 반올림 (Scale: 2, RoundingMode.HALF_UP)
 *
 * 🛡️ [Domain Driven Design (DDD) & Hexagonal Architecture 관점]
 * - 이 Enum은 도메인 모델 내에서 통화 관련 반올림 비즈니스 규칙을 캡슐화합니다.
 * - Controller나 Adapter 등 외부 계층에 의존하지 않으며, 전표 전송 직전 및 금액 연산 결과 정밀도 제어에 활용됩니다.
 */
public enum CurrencyRoundingPolicy {
    /** 원화: 1원 미만 절사 (소수점 0자리, FLOOR) */
    KRW("KRW", 0, RoundingMode.FLOOR),

    /** 엔화: 1엔 미만 절사 (소수점 0자리, FLOOR) */
    JPY("JPY", 0, RoundingMode.FLOOR),

    /** 달러: Cents 단위 반올림 (소수점 2자리, HALF_UP) */
    USD("USD", 2, RoundingMode.HALF_UP),

    /** 유로: Cents 단위 반올림 (소수점 2자리, HALF_UP) */
    EUR("EUR", 2, RoundingMode.HALF_UP),

    /** 파운드: Penny 단위 반올림 (소수점 2자리, HALF_UP) */
    GBP("GBP", 2, RoundingMode.HALF_UP);

    private final String currencyCode;
    private final int scale;
    private final RoundingMode roundingMode;

    CurrencyRoundingPolicy(String currencyCode, int scale, RoundingMode roundingMode) {
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

    /**
     * 통화 코드를 바탕으로 해당 통화의 반올림/절사 정책을 조회합니다.
     *
     * @param currencyCode ISO 통화 코드 (예: KRW, USD, EUR, JPY)
     * @return 매칭되는 CurrencyRoundingPolicy, 없거나 null/blank일 경우 기본값 KRW 반환
     */
    public static CurrencyRoundingPolicy of(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return KRW;
        }
        for (CurrencyRoundingPolicy policy : values()) {
            if (policy.currencyCode.equalsIgnoreCase(currencyCode.trim())) {
                return policy;
            }
        }
        return KRW;
    }

    /**
     * 지정된 금액에 통화별 절사/반올림 규칙을 적용하여 정제된 BigDecimal을 반환합니다.
     *
     * @param amount 원본 금액 (null일 경우 BigDecimal.ZERO에 정책 scale/roundingMode 적용)
     * @return 반올림/절사가 적용된 BigDecimal
     */
    public BigDecimal applyRounding(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(scale, roundingMode);
        }
        return amount.setScale(scale, roundingMode);
    }
}
