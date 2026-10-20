package com.ho.account.masterdata.core.application.query;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;

/** Columns needed by list responses; child accounts are deliberately absent. */
public record BusinessPartnerSummary(
        Long id, String businessPartnerCode, String businessPartnerName,
        String registrationNumber, String ceoName, String businessType, String businessItem,
        BusinessPartner.PartnerType partnerType, Boolean useYn,
        BusinessPartner.KycStatus kycStatus, BusinessPartner.RiskRating riskRating,
        LocalDate validFrom, LocalDate validTo) { }
