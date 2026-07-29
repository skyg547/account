package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;

/**
 * 거래처 등록/수정 요청을 위한 Application Command 클래스입니다.
 *
 * <p>REST 요청 DTO로부터 변환되어 서비스 계층으로 전달되며, API 스펙과 
 * 도메인 모델 사이의 완충 역할을 수행합니다.</p>
 */
public record BusinessPartnerCommand(
        String businessPartnerCode,
        String businessPartnerName,
        String registrationNumber,
        String ceoName,
        String businessType,
        String businessItem,
        BusinessPartner.PartnerType partnerType,
        Boolean useYn,
        BusinessPartner.KycStatus kycStatus,
        BusinessPartner.RiskRating riskRating,
        LocalDate validFrom,
        LocalDate validTo) {

    public BusinessPartner toEntity() {
        BusinessPartner entity = new BusinessPartner();
        entity.setBusinessPartnerCode(businessPartnerCode);
        entity.setBusinessPartnerName(businessPartnerName);
        entity.setRegistrationNumber(registrationNumber);
        entity.setCeoName(ceoName);
        entity.setBusinessType(businessType);
        entity.setBusinessItem(businessItem);
        entity.setPartnerType(partnerType);
        entity.setUseYn(useYn);
        entity.setKycStatus(kycStatus);
        entity.setRiskRating(riskRating);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        return entity;
    }
}
