package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningSubmission;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** Screening evidence submitted for a partner; the requester comes from verified JWT identity. */
public record BusinessPartnerScreeningSubmissionDto(
        @NotBlank String jurisdiction,
        @NotBlank String identifierType,
        @NotBlank String identifier,
        @NotBlank String evidenceReference,
        LocalDate evidenceExpiresOn) {

    public BusinessPartnerScreeningSubmission toCommand(String businessPartnerCode, String submittedBy) {
        return new BusinessPartnerScreeningSubmission(
                businessPartnerCode, jurisdiction, identifierType, identifier, evidenceReference,
                evidenceExpiresOn, submittedBy);
    }
}
