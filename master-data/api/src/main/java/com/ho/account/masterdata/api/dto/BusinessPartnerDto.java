package com.ho.account.masterdata.api.dto;

import com.ho.account.common.Masked;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;

/**
 * 거래처 응답 DTO
 */
public class BusinessPartnerDto {

    private final Long id;
    private final String businessPartnerCode;
    private final String businessPartnerName;

    /*
     * 도메인은 사업자번호 원문으로 업무 규칙을 수행하고 영속성 계층은 그 값을 저장하는 책임만 가집니다.
     * 마스킹은 외부 JSON을 만드는 순간의 표현 정책이므로 응답 DTO 경계에 두어, 내부 값은 훼손하지
     * 않으면서 어떤 API 경로로 응답하더라도 동일한 개인정보 보호 규칙을 적용합니다.
     */
    @Masked(pattern = "REG_NO")
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

    /**
     * 영속성 엔티티가 아니라 유즈케이스가 반환한 순수 거래처 도메인을 API 응답으로 변환합니다.
     */
    public static BusinessPartnerDto fromDomain(BusinessPartner domain) {
        return new BusinessPartnerDto(
                domain.getId(),
                domain.getBusinessPartnerCode(),
                domain.getBusinessPartnerName(),
                domain.getRegistrationNumber(),
                domain.getCeoName(),
                domain.getBusinessType(),
                domain.getBusinessItem(),
                domain.getPartnerType(),
                domain.getUseYn(),
                domain.getKycStatus(),
                domain.getRiskRating(),
                domain.getValidFrom(),
                domain.getValidTo());
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
