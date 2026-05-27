package com.ho.account.mart.core.infrastructure.persistence.entity.marketdata;

import com.ho.account.shared.finance.enums.CurrencyCode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Market Data] 시장 금리 (Market Rate) 엔티티.
 * CD금리, KORIBOR, 국고채 수익률 등 시장에서 공시되는 각종 기준 금리 정보를 관리합니다.
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 시장 금리란 금융시장에서 돈을 빌리거나 빌려줄 때 기준이 되는 '가격'입니다.
 * 은행 대출이나 예금 금리를 정할 때 이 시장 금리에 마진(Spread)을 더해 최종 금리를 결정합니다.
 * 재무 결산 시스템은 이 금리들이 변하는 것을 보고 우리 자산의 가치가 얼마나 변할지 예측합니다.
 */
@Entity
@Table(name = "market_rate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기준일자 (금리가 공시된 날) */
    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDate;

    /** 금리 명칭 (예: CD91일, KORIBOR 3M, 국고채 3년, 국고채 10년 등) */
    @Column(name = "rate_name", nullable = false, length = 50)
    private String rateName;

    /** 금리 유형 (BENCHMARK, GOVERNMENT, CORPORATE, SWAP 등) */
    @Column(name = "rate_type", length = 20)
    private String rateType;

    /** 통화 코드 */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 3)
    private CurrencyCode currency;

    /** 만기 (개월 단위). 예: 3 = 3개월, 12 = 1년, 120 = 10년 */
    @Column(name = "tenor_months")
    private Integer tenorMonths;

    /** 만기 라벨 (예: "3M", "1Y", "10Y") */
    @Column(name = "tenor_label", length = 10)
    private String tenorLabel;

    /** 고시 금리 (%) */
    @Column(name = "rate", precision = 10, scale = 6, nullable = false)
    private BigDecimal rate;

    /** 전일 대비 변동폭 (bp, 1bp = 0.01%) */
    @Column(name = "change_bp", precision = 10, scale = 2)
    private BigDecimal changeBp;

    /** 데이터 출처 (예: 한국은행, 금융투자협회, Bloomberg 등) */
    @Column(name = "source", length = 50)
    private String source;

    /** 데이터 활성 여부 */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /** 데이터 생성 일시 */
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
