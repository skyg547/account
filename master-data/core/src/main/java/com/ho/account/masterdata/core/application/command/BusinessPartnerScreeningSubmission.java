package com.ho.account.masterdata.core.application.command;

import java.time.LocalDate;

/** Evidence submitted by an authenticated actor for a stable partner code. */
public record BusinessPartnerScreeningSubmission(
        String businessPartnerCode,
        String jurisdiction,
        String identifierType,
        String identifier,
        String evidenceReference,
        LocalDate evidenceExpiresOn,
        String submittedBy) {
}
