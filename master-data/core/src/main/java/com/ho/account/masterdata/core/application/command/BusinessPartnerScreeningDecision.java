package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;

/** Human reviewer decision on a pending screening. */
public record BusinessPartnerScreeningDecision(
        String businessPartnerCode,
        BusinessPartner.KycStatus status,
        BusinessPartner.RiskRating riskRating,
        String decisionReason,
        String reviewedBy) {
}
