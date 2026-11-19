package com.ho.account.masterdata.api.web.dto;

import jakarta.validation.constraints.NotNull;
import com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus;

/** Exact status pair for a signed operation identity carried in the path. */
public record FiscalPeriodTransitionRequestDto(
        @NotNull FiscalPeriodClosingStatus expectedStatus,
        @NotNull FiscalPeriodClosingStatus targetStatus) {
}
