package com.ho.account.mart.core.infrastructure.persistence.entity.marketdata;

import com.ho.account.shared.finance.enums.CurrencyCode;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [시장 데이터] 수익률 곡선(Yield Curve) 마스터 엔티티
 * 
 * 💡 [초보자를 위한 금융 개념 설명]
 * '수익률 곡선(Yield Curve)'이란, 돈을 빌리는 기간(만기)에 따라 금리가 어떻게 변하는지를 
 * 선으로 연결한 그래프입니다. 
 * 예를 들어, 1년 빌릴 때의 금리, 10년 빌릴 때의 금리를 점으로 찍고 선으로 이은 것입니다. 
 * 재무 결산 시스템에서 이 곡선은 '미래의 돈이 현재 얼마의 가치가 있는지'를 계산하는 핵심 '잣대'입니다.
 */
@Entity
@Table(name = "market_yield_curve")
public class YieldCurveEntity {

    /** 내부 관리용 유일 식별자 */
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @Column(name = "base_dt", nullable = false) 
    @Comment("분석기준일자: 시장 금리 상황을 관측한 날짜")
    private LocalDate baseDate;

    @Enumerated(EnumType.STRING) 
    @Column(name = "currency") 
    @Comment("통화코드: 곡선의 대상 통화 (KRW, USD 등)")
    private CurrencyCode currency;

    @Column(name = "curve_name") 
    @Comment("곡선명칭: 금리 곡선의 종류 (예: 국고채, IRS, 리보 등)")
    private String curveName;

    @Column(name = "description") 
    @Comment("비고/설명: 해당 곡선의 출처 또는 용도")
    private String description;

    @Column(name = "is_active") 
    @Comment("활성상태: 산출 엔진에서 사용 여부")
    private Boolean isActive = true;

    /** 
     * 수익률 곡선 지점(Points) 리스트 
     * 💡 [개념 설명] 
     * 곡선은 특정 만기(Tenor)의 점들로 구성되지만, 실제 대출의 만기는 그 점들 사이에 존재할 수 있습니다.
     * 이때 '보간법(Interpolation)'을 사용하여 점 사이의 값을 추정하며, 이는 대손충당금(IFRS9) 산출의 정밀도를 결정합니다.
     */
    @OneToMany(mappedBy = "yieldCurve", cascade = CascadeType.ALL, orphanRemoval = true) 
    private List<YieldCurvePointEntity> points = new ArrayList<>();

    @Column(name = "created_at") 
    @Comment("데이터생성일시")
    private LocalDateTime createdAt = LocalDateTime.now();

    public YieldCurveEntity() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getBaseDate() { return baseDate; }
    public void setBaseDate(LocalDate val) { this.baseDate = val; }
    public CurrencyCode getCurrency() { return currency; }
    public void setCurrency(CurrencyCode val) { this.currency = val; }
    public String getCurveName() { return curveName; }
    public void setCurveName(String val) { this.curveName = val; }
    public String getDescription() { return description; }
    public void setDescription(String val) { this.description = val; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean val) { this.isActive = val; }
    public List<YieldCurvePointEntity> getPoints() { return points; }
    public void setPoints(List<YieldCurvePointEntity> val) { this.points = val; }
    public void addPoint(YieldCurvePointEntity point) { points.add(point); point.setYieldCurve(this); }

    /** [빌더 패턴] 객체 생성을 위한 도우미 클래스 */
    public static class Builder {
        private final YieldCurveEntity entity = new YieldCurveEntity();
        public Builder id(Long val) { entity.id = val; return this; }
        public Builder baseDate(LocalDate val) { entity.baseDate = val; return this; }
        public Builder currency(CurrencyCode val) { entity.currency = val; return this; }
        public Builder curveName(String val) { entity.curveName = val; return this; }
        public Builder description(String val) { entity.description = val; return this; }
        public Builder isActive(boolean val) { entity.isActive = val; return this; }
        public YieldCurveEntity build() { return entity; }
    }
    public static Builder builder() { return new Builder(); }
}
