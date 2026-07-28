package com.ho.account.internalaudit.core.domain.evaluation;

import lombok.Builder;

@Builder
public record Deficiency(
        String deficiencyId,
        String evaluationId,
        String description,
        String remediationPlan,
        String status // IDENTIFIED, REMEDIATING, CLOSED
) {}
