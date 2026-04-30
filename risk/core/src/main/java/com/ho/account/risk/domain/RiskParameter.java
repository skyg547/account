package com.ho.account.risk.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [RiskParameter]
 * 규제(Basel III) 또는 내부 모형에 따른 리스크 파라미터 정의 엔티티.
 * 자산 클래스별 위험가중치(RW), PD/LGD 가이드라인 등을 관리합니다.
 */
@Entity
@Table(name = "risk_parameters")
public class RiskParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String parameterCategory; // RW(위험가중치), PD_GUIDE, LGD_GUIDE 등

    @Column(nullable = false, length = 50)
    private String assetClass; // CORPORATE, RETAIL, RESIDENTIAL_MORTGAGE 등

    @Column(nullable = false, length = 20)
    private String approachType; // SA(표준방법), IRB(내부등급법)

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal parameterValue; // 실제 파라미터 값

    @Column(nullable = false)
    private LocalDate validFrom; // 적용 시작일

    @Column(nullable = false)
    private LocalDate validTo; // 적용 종료일

    @Column(length = 255)
    private String description;

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getParameterCategory() { return parameterCategory; }
    public void setParameterCategory(String parameterCategory) { this.parameterCategory = parameterCategory; }

    public String getAssetClass() { return assetClass; }
    public void setAssetClass(String assetClass) { this.assetClass = assetClass; }

    public String getApproachType() { return approachType; }
    public void setApproachType(String approachType) { this.approachType = approachType; }

    public BigDecimal getParameterValue() { return parameterValue; }
    public void setParameterValue(BigDecimal parameterValue) { this.parameterValue = parameterValue; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
