package com.ho.account.internalaudit.core.domain.evaluation;

import lombok.Builder;

@Builder
public record DesignEvaluation(
        String evaluationId,
        String controlId,
        String evaluatorId,
        String evaluationDate,
        String result, // EFFECTIVE, INEFFECTIVE
        String remarks
) {}
