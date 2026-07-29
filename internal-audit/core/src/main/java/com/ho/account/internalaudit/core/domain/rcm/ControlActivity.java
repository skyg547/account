package com.ho.account.internalaudit.core.domain.rcm;

import lombok.Builder;

@Builder
public record ControlActivity(
        String controlId,
        String riskId,
        String controlDescription,
        String controlType, // PREVENTIVE, DETECTIVE
        String executionMethod, // MANUAL, AUTOMATED, IT_DEPENDENT_MANUAL
        String frequency, // DAILY, WEEKLY, MONTHLY, QUARTERLY, ANNUALLY
        String ownerId
) {}
