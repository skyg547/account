package com.ho.account.risk.domain;

import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [MarketRiskExposure]
 * 시장리스크 노출 현황 엔티티.
 * 환율, 금리 등 시장 변수 변동에 따른 자산/부채의 가치 변동 위험을 관리합니다.
 */
@Entity
@Table(name = "market_risk_exposures")
@Getter @Setter
public class MarketRiskExposure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate baseDate; // 기준일자

    @Column(nullable = false, length = 50)
    private String instrumentType; // 상품 유형 (FX_POSITION, BOND, DERIVATIVE 등)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency; // 대상 통화

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal positionAmount; // 포지션 금액 (외화 기준)

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal marketPrice; // 현재 시장 가격 (환율 등)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseCurrencyAmount; // 장부금액 (기본 통화 환산)

    @Column(precision = 19, scale = 2)
    private BigDecimal varAmount; // VaR (Value at Risk) 산출값

    @Column(length = 20)
    private String riskFactor; // 주요 위험 요인 (FX, INTEREST_RATE, EQUITY)

    /**
     * 기본 통화 환산 금액 계산
     */
    public void convertToBaseCurrency(BigDecimal exchangeRate) {
        if (positionAmount != null && exchangeRate != null) {
            this.baseCurrencyAmount = positionAmount.multiply(exchangeRate);
            this.marketPrice = exchangeRate;
        }
    }
}
