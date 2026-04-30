package com.ho.account.risk.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [CreditRiskExposure]
 * 신용리스크 익스포저 엔티티. 
 * Basel III RWA 산출의 기초가 되는 PD, LGD, EAD 데이터를 포함하는 RDM의 핵심 엔티티입니다.
 */
@Entity
@Table(name = "credit_risk_exposures")
public class CreditRiskExposure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate baseDate; // 기준일자

    @Column(nullable = false, length = 50)
    private String sourceSystemId; // 원천시스템 구분 (LOAN, LEASE 등)

    @Column(nullable = false, length = 50)
    private String sourceReferenceId; // 원천시스템의 참조 ID (대출번호 등)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_id", nullable = false)
    private BusinessPartner businessPartner; // 차주 정보

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal eadAmount; // 부도시 익스포저 (Exposure at Default)

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal pdRate; // 부도확률 (Probability of Default)

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal lgdRate; // 부도시 손실률 (Loss Given Default)

    @Column(precision = 19, scale = 2)
    private BigDecimal elAmount; // 예상손실 (Expected Loss = EAD * PD * LGD)

    @Column(length = 30)
    private String assetClass; // 자산건전성 분류 (기업, 소매, 주담대 등)

    // 비즈니스 로직: 예상손실 계산
    public void calculateExpectedLoss() {
        if (eadAmount != null && pdRate != null && lgdRate != null) {
            this.elAmount = eadAmount.multiply(pdRate).multiply(lgdRate);
        }
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getBaseDate() { return baseDate; }
    public void setBaseDate(LocalDate baseDate) { this.baseDate = baseDate; }

    public String getSourceSystemId() { return sourceSystemId; }
    public void setSourceSystemId(String sourceSystemId) { this.sourceSystemId = sourceSystemId; }

    public String getSourceReferenceId() { return sourceReferenceId; }
    public void setSourceReferenceId(String sourceReferenceId) { this.sourceReferenceId = sourceReferenceId; }

    public BusinessPartner getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(BusinessPartner businessPartner) { this.businessPartner = businessPartner; }

    public BigDecimal getEadAmount() { return eadAmount; }
    public void setEadAmount(BigDecimal eadAmount) { this.eadAmount = eadAmount; }

    public BigDecimal getPdRate() { return pdRate; }
    public void setPdRate(BigDecimal pdRate) { this.pdRate = pdRate; }

    public BigDecimal getLgdRate() { return lgdRate; }
    public void setLgdRate(BigDecimal lgdRate) { this.lgdRate = lgdRate; }

    public BigDecimal getElAmount() { return elAmount; }
    public void setElAmount(BigDecimal elAmount) { this.elAmount = elAmount; }

    public String getAssetClass() { return assetClass; }
    public void setAssetClass(String assetClass) { this.assetClass = assetClass; }
}
