package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;

/**
 * 嫄곕옒泥?REST ?묐떟 DTO?낅땲??
 *
 * <p>?묐떟?먮뒗 ?붾㈃怨??몃? 紐⑤뱢???꾩슂???덉젙 ?꾨뱶留??댁뒿?덈떎. 怨꾩쥖 紐⑸줉 媛숈? 誘쇨컧?섍굅?? * 臾닿굅???곌?愿怨꾨뒗 蹂꾨룄 API濡?遺꾨━?섎뒗 寃껋씠 ?덉쟾?⑸땲??</p>
 */
public class BusinessPartnerDto {

    private final Long id;
    private final String businessPartnerCode;
    private final String businessPartnerName;
    private final String registrationNumber;
    private final String ceoName;
    private final String businessType;
    private final String businessItem;
    private final BusinessPartner.PartnerType partnerType;
    private final Boolean useYn;
    private final BusinessPartner.KycStatus kycStatus;
    private final BusinessPartner.RiskRating riskRating;
    private final LocalDate validFrom;
    private final LocalDate validTo;

    public BusinessPartnerDto(Long id, String businessPartnerCode, String businessPartnerName,
            String registrationNumber, String ceoName, String businessType, String businessItem,
            BusinessPartner.PartnerType partnerType, Boolean useYn, BusinessPartner.KycStatus kycStatus,
            BusinessPartner.RiskRating riskRating, LocalDate validFrom, LocalDate validTo) {
        this.id = id;
        this.businessPartnerCode = businessPartnerCode;
        this.businessPartnerName = businessPartnerName;
        this.registrationNumber = registrationNumber;
        this.ceoName = ceoName;
        this.businessType = businessType;
        this.businessItem = businessItem;
        this.partnerType = partnerType;
        this.useYn = useYn;
        this.kycStatus = kycStatus;
        this.riskRating = riskRating;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public static BusinessPartnerDto fromEntity(BusinessPartner entity) {
        return new BusinessPartnerDto(
                entity.getId(),
                entity.getBusinessPartnerCode(),
                entity.getBusinessPartnerName(),
                entity.getRegistrationNumber(),
                entity.getCeoName(),
                entity.getBusinessType(),
                entity.getBusinessItem(),
                entity.getPartnerType(),
                entity.getUseYn(),
                entity.getKycStatus(),
                entity.getRiskRating(),
                entity.getValidFrom(),
                entity.getValidTo());
    }

    public Long getId() { return id; }
    public String getBusinessPartnerCode() { return businessPartnerCode; }
    public String getBusinessPartnerName() { return businessPartnerName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getCeoName() { return ceoName; }
    public String getBusinessType() { return businessType; }
    public String getBusinessItem() { return businessItem; }
    public BusinessPartner.PartnerType getPartnerType() { return partnerType; }
    public Boolean getUseYn() { return useYn; }
    public BusinessPartner.KycStatus getKycStatus() { return kycStatus; }
    public BusinessPartner.RiskRating getRiskRating() { return riskRating; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
}
