package com.ho.account.internalaudit.core.domain.rcm;

import lombok.Builder;

@Builder
public record RcmRisk(
        String riskId,
        String processId,
        String riskDescription,
        String impactLevel,
        String likelihood
) {}
