package com.ho.account.mart.core.infrastructure.persistence.entity.marketdata;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * [시장 데이터] 수익률 곡선 지점(Yield Curve Point) 엔티티
 * 
 * 💡 [초보자를 위한 금융 개념 설명]
 * '수익률 곡선 지점'은 금리 곡선을 그리기 위한 실제 '데이터 포인트'들입니다. 
 * 예를 들어, '국고채 3년물 금리는 3.5%'라는 정보가 있다면, 
 * 여기서 '3년'은 만기(Tenor)가 되고 '3.5%'는 금리(Rate)가 됩니다. 
 * 이러한 점들을 여러 개 연결하면 하나의 매끄러운 곡선(Yield Curve)이 완성되며, 
 * 이를 통해 만기가 3년 2개월인 상품의 금리도 추정할 수 있게 됩니다.
 */
@Entity
@Table(name = "market_yield_curve_point")
public class YieldCurvePointEntity {

    /** 내부 관리용 유일 식별자 */
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    /** 
     * 소속 수익률 곡선 - 이 지점이 어떤 금리 곡선(예: 국고채곡선)에 속하는지 나타냅니다. 
     * 부모인 YieldCurve 엔티티와 연결됩니다.
     */
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "curve_id") 
    private YieldCurveEntity yieldCurve;

    /** 만기 라벨 - 사람이 읽기 쉬운 만기 표시입니다. (예: "3M", "1Y", "10Y") */
    @Column(name = "tenor_label", length = 10)
    private String tenorLabel;

    /** 만기 개월수 - 계산을 위해 만기를 개월 단위 숫자로 바꾼 값입니다. (예: "1Y" -> 12) */
    @Column(name = "tenor_months") 
    private Integer tenorMonths;

    /** 금리 - 해당 만기 시점의 시장 이자율(%)입니다. */
    @Column(name = "rate", precision = 10, scale = 6)
    private BigDecimal rate;

    /** 
     * 할인 계수 (Discount Factor) 
     * 💡 [개념 설명] 미래의 1원을 현재 가치로 바꿀 때 곱하는 숫자입니다. 
     * 보통 금리가 높을수록 할인 계수는 작아집니다. (예: 1년 뒤 1원의 현재 가치는 0.96원)
     */
    @Column(name = "discount_factor", precision = 19, scale = 12)
    private BigDecimal discountFactor;

    public YieldCurvePointEntity() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenorLabel() { return tenorLabel; }
    public Integer getTenorMonths() { return tenorMonths; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getDiscountFactor() { return discountFactor; }
    public void setYieldCurve(YieldCurveEntity val) { this.yieldCurve = val; }
    
    /** [빌더 패턴] 객체 생성을 위한 도우미 클래스 */
    public static class Builder {
        private final YieldCurvePointEntity entity = new YieldCurvePointEntity();
        public Builder id(Long val) { entity.id = val; return this; }
        public Builder tenorLabel(String val) { entity.tenorLabel = val; return this; }
        public Builder tenorMonths(Integer val) { entity.tenorMonths = val; return this; }
        public Builder rate(BigDecimal val) { entity.rate = val; return this; }
        public Builder discountFactor(BigDecimal val) { entity.discountFactor = val; return this; }
        public YieldCurvePointEntity build() { return entity; }
    }
    public static Builder builder() { return new Builder(); }
}
