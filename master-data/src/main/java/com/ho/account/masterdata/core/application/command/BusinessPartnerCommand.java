package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;

/**
 * 嫄곕옒泥??앹꽦/?섏젙 ?좎뒪耳?댁뒪濡??ㅼ뼱?ㅻ뒗 application command?낅땲??
 *
 * <p>REST ?붿껌 DTO瑜?洹몃?濡??쒕퉬?ㅼ뿉 ?섍린吏 ?딄퀬 command濡?蹂?섑븯硫? API 紐⑥뼇??諛붾뚯뼱?? * core application 怨꾩링???섎룄媛 ?붾뱾由ъ? ?딆뒿?덈떎.</p>
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
