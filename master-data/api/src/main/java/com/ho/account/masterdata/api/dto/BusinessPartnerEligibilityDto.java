package com.ho.account.masterdata.api.dto;

import java.time.LocalDate;

/** Explicit eligibility result without screening identifiers or evidence. */
public record BusinessPartnerEligibilityDto(
        String businessPartnerCode, LocalDate asOfDate, boolean eligible) {
}
