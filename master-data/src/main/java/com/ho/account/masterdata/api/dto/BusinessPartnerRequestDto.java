package com.ho.account.masterdata.api.dto;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import java.time.LocalDate;

/**
 * 거래처 REST 요청 DTO입니다.
 *
 * <p>Controller 바깥으로만 쓰는 API 모델입니다. JPA Entity를 요청 본문으로 직접 받지 않기
 * 때문에 DB 컬럼이나 연관관계가 API 계약으로 새어 나가지 않습니다.</p>
 */
public class BusinessPartnerRequestDto {

    private String businessPartnerCode;
    private String businessPartnerName;
    private String registrationNumber;
    private String ceoName;
    private String businessType;
    private String businessItem;
    private BusinessPartner.PartnerType partnerType;
    private Boolean useYn;
    private BusinessPartner.KycStatus kycStatus;
    private BusinessPartner.RiskRating riskRating;
    private LocalDate validFrom;
    private LocalDate validTo;

    public BusinessPartnerCommand toCommand() {
        return new BusinessPartnerCommand(
                businessPartnerCode,
                businessPartnerName,
                registrationNumber,
                ceoName,
                businessType,
                businessItem,
                partnerType,
                useYn,
                kycStatus,
                riskRating,
                validFrom,
                validTo);
    }

    public String getBusinessPartnerCode() { return businessPartnerCode; }
    public void setBusinessPartnerCode(String businessPartnerCode) { this.businessPartnerCode = businessPartnerCode; }
    public String getBusinessPartnerName() { return businessPartnerName; }
    public void setBusinessPartnerName(String businessPartnerName) { this.businessPartnerName = businessPartnerName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getCeoName() { return ceoName; }
    public void setCeoName(String ceoName) { this.ceoName = ceoName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessItem() { return businessItem; }
    public void setBusinessItem(String businessItem) { this.businessItem = businessItem; }
    public BusinessPartner.PartnerType getPartnerType() { return partnerType; }
    public void setPartnerType(BusinessPartner.PartnerType partnerType) { this.partnerType = partnerType; }
    public Boolean getUseYn() { return useYn; }
    public void setUseYn(Boolean useYn) { this.useYn = useYn; }
    public BusinessPartner.KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(BusinessPartner.KycStatus kycStatus) { this.kycStatus = kycStatus; }
    public BusinessPartner.RiskRating getRiskRating() { return riskRating; }
    public void setRiskRating(BusinessPartner.RiskRating riskRating) { this.riskRating = riskRating; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
}
