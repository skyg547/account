package com.ho.account.journal.core.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 다통화 은행 시스템에서 외환(FX, Foreign Exchange) 포지션을 관리하는 도메인 모델입니다.
 * 
 * <p><b>초보자 가이드(Beginner's Guide):</b></p>
 * <p>
 * 외화 대출이나 외화 예금을 취급할 때, 원장(Ledger)에는 외화 원금(예: USD 1,000)뿐만 아니라
 * 이를 당시 환율로 환산한 원화 금액(예: KRW 1,300,000)이 함께 기록되어야 합니다.
 * 환율은 매일 변동하기 때문에, 매월 말일(또는 매일 EOD)에 '외화 평가(FX Revaluation)'라는 
 * 배치를 돌려서, 최신 환율 기준으로 원화 금액을 다시 계산하고 그 차액을 '외환평가손익'으로 장부에 반영합니다.
 * 이 클래스는 그러한 외화 금액과 원화 환산액의 비율(포지션)을 추적합니다.
 * </p>
 */
public class FxPosition {

    private final String currencyCode; // 예: "USD", "EUR"
    private BigDecimal foreignAmount;  // 외화 금액
    private BigDecimal baseAmount;     // 원화(기축통화) 환산 금액

    public FxPosition(String currencyCode, BigDecimal foreignAmount, BigDecimal baseAmount) {
        this.currencyCode = currencyCode;
        this.foreignAmount = foreignAmount != null ? foreignAmount : BigDecimal.ZERO;
        this.baseAmount = baseAmount != null ? baseAmount : BigDecimal.ZERO;
    }

    /**
     * 특정 환율을 적용하여 외화 평가(Revaluation)를 수행합니다.
     * 
     * @param currentExchangeRate 현재 환율 (예: 1 USD 당 1350.00 KRW)
     * @return 평가에 따른 원화(Base Currency) 차액 (이 금액만큼 분개를 쳐야 함)
     */
    public BigDecimal revaluate(BigDecimal currentExchangeRate) {
        if (foreignAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 새로운 원화 환산액 = 외화 잔액 * 현재 환율
        BigDecimal newBaseAmount = foreignAmount.multiply(currentExchangeRate)
                .setScale(0, RoundingMode.HALF_UP); // 원화는 보통 소수점 없이 반올림 처리

        // 평가 차액 = 새 원화 금액 - 기존 원화 금액
        BigDecimal difference = newBaseAmount.subtract(this.baseAmount);

        // 평가 결과로 장부의 환산 금액 업데이트
        this.baseAmount = newBaseAmount;

        return difference;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getForeignAmount() {
        return foreignAmount;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }
}
