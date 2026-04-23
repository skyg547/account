package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;

/**
 * „ê³•?’ï???¹ê½¦/??ì ™ ?ë’ª?³Â??ë’ª???¼ë¼±??»ë’— application command??…ë•²??
 *
 * <p>REST ?¿ê»Œ DTO??¹ëªƒ????•í‰¬??¼ë¿‰ ??ëÂ ??„í?command?‚Â??‘ë¸¯? API â‘¥??›ë¶¾???¼±?? * core application ?¾©????ë£„›Â ?¾ë±¾?±Ñ? ??†ë’¿??ˆë–.</p>
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
