package com.ho.account.risk.dto;

import com.ho.account.risk.domain.RiskWeightedAsset;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RiskWeightedAssetDto {
    private Long id;
    private String sourceReferenceId;
    private String businessPartnerName;
    private LocalDate calculationDate;
    private String approachType;
    private BigDecimal eadAmount;
    private BigDecimal riskWeight;
    private BigDecimal rwaAmount;

    public static RiskWeightedAssetDto fromEntity(RiskWeightedAsset entity) {
        RiskWeightedAssetDto dto = new RiskWeightedAssetDto();
        dto.id = entity.getId();
        dto.sourceReferenceId = entity.getExposure().getSourceReferenceId();
        dto.businessPartnerName = entity.getExposure().getBusinessPartner().getBusinessPartnerName();
        dto.calculationDate = entity.getCalculationDate();
        dto.approachType = entity.getApproachType();
        dto.eadAmount = entity.getExposure().getEadAmount();
        dto.riskWeight = entity.getRiskWeight();
        dto.rwaAmount = entity.getRwaAmount();
        return dto;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public String getSourceReferenceId() { return sourceReferenceId; }
    public String getBusinessPartnerName() { return businessPartnerName; }
    public LocalDate getCalculationDate() { return calculationDate; }
    public String getApproachType() { return approachType; }
    public BigDecimal getEadAmount() { return eadAmount; }
    public BigDecimal getRiskWeight() { return riskWeight; }
    public BigDecimal getRwaAmount() { return rwaAmount; }
}
