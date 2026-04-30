package com.ho.account.risk.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [RiskWeightedAsset]
 * 위험가중자산(RWA) 산출 결과 엔티티.
 * 개별 익스포저에 대해 적용된 위험가중치(RW)와 최종 산출된 RWA 금액을 관리합니다.
 */
@Entity
@Table(name = "risk_weighted_assets")
public class RiskWeightedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exposure_id", nullable = false)
    private CreditRiskExposure exposure;

    @Column(nullable = false)
    private LocalDate calculationDate; // 산출일자

    @Column(nullable = false, length = 20)
    private String approachType; // 산출방법 (SA: 표준방법, IRB: 내부등급법)

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal riskWeight; // 위험가중치 (Risk Weight)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal rwaAmount; // 위험가중자산 금액 (RWA = EAD * RW)

    // 비즈니스 로직: RWA 계산
    public void calculateRwa() {
        if (exposure != null && riskWeight != null) {
            this.rwaAmount = exposure.getEadAmount().multiply(riskWeight);
        }
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CreditRiskExposure getExposure() { return exposure; }
    public void setExposure(CreditRiskExposure exposure) { this.exposure = exposure; }

    public LocalDate getCalculationDate() { return calculationDate; }
    public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }

    public String getApproachType() { return approachType; }
    public void setApproachType(String approachType) { this.approachType = approachType; }

    public BigDecimal getRiskWeight() { return riskWeight; }
    public void setRiskWeight(BigDecimal riskWeight) { this.riskWeight = riskWeight; }

    public BigDecimal getRwaAmount() { return rwaAmount; }
    public void setRwaAmount(BigDecimal rwaAmount) { this.rwaAmount = rwaAmount; }
}
