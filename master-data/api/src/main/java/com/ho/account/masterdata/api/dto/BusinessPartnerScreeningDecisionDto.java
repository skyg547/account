package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningDecision;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A review decision; the reviewer comes from verified JWT identity, never from the body. */
public record BusinessPartnerScreeningDecisionDto(
        @NotNull BusinessPartner.KycStatus status,
        @NotNull BusinessPartner.RiskRating riskRating,
        @NotBlank @Size(max = 1000) String decisionReason) {

    public BusinessPartnerScreeningDecision toCommand(String businessPartnerCode, String reviewedBy) {
        return new BusinessPartnerScreeningDecision(
                businessPartnerCode, status, riskRating, decisionReason, reviewedBy);
    }
}
