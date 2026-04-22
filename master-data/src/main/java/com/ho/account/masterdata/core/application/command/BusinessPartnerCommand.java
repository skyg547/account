package com.ho.account.masterdata.core.application.command;

import com.ho.account.basic.domain.BusinessPartner;
import java.time.LocalDate;

/**
 * 거래처 생성/수정 유스케이스로 들어오는 application command입니다.
 *
 * <p>REST 요청 DTO를 그대로 서비스에 넘기지 않고 command로 변환하면, API 모양이 바뀌어도
 * core application 계층의 의도가 흔들리지 않습니다.</p>
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
