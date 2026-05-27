package com.ho.account.mart.core.infrastructure.persistence.entity.marketdata;

import com.ho.account.shared.finance.enums.CurrencyCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Entity] 환율 (Exchange Rate) 엔티티.
 * 기준 통화(Base) 대비 상대 통화(Quote)의 환율 정보를 저장하며, 다통화 기반 리스크 포지션 통합 시 필수적인 데이터입니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 테이블은 "1달러가 한국 돈으로 얼마인가 "를 알려주는 기준표입니다.
 * 은행은 전 세계 다양한 통화로 대출을 해주기 때문에, 리스크를 통합해서 관리하려면
 * 모든 돈을 하나의 통화(예: KRW)로 환산해서 합산해야 합니다.
 * 이때 사용하는 것이 환율 정보이며, 매일매일 시장 정보를 수신하여 업데이트합니다.
 */
@Entity
@Table(name = "market_exchange_rate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_dt", nullable = false)
    @Comment("기준일자: 환율을 관측한 영업일")
    private LocalDate baseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false, length = 3)
    @Comment("기준통화: 환율의 기초가 되는 통화 (예: 1 USD)")
    private CurrencyCode baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_currency", nullable = false, length = 3)
    @Comment("상대통화: 기준통화 1단위에 대응하는 대상 통화 (예: KRW)")
    private CurrencyCode quoteCurrency;

    @Column(name = "base_rate", precision = 19, scale = 4, nullable = false)
    @Comment("매매기준율: 대손충당금(IFRS9) 산출 시 원화(LCY) 환산에 사용하는 표준 환율")
    private BigDecimal baseRate;

    @Column(name = "bid_rate", precision = 19, scale = 4)
    @Comment("장부매입율: 은행이 고객으로부터 외화를 살 때 적용하는 환율")
    private BigDecimal bidRate;

    @Column(name = "ask_rate", precision = 19, scale = 4)
    @Comment("장부매도율: 은행이 고객에게 외화를 팔 때 적용하는 환율")
    private BigDecimal askRate;

    @Column(name = "change_amt", precision = 10, scale = 4)
    @Comment("변동액: 전일 대비 환율의 절대 변화량")
    private BigDecimal changeAmount;

    @Column(name = "change_rate", precision = 10, scale = 4)
    @Comment("변동률: 전일 대비 환율의 변화 백분율 (%)")
    private BigDecimal changeRate;

    @Column(name = "source", length = 50)
    @Comment("데이터출처: 한국은행, 로이터, 블룸버그 등 정보 제공처")
    private String source;

    @Column(name = "created_at", updatable = false)
    @Comment("생성일시")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
