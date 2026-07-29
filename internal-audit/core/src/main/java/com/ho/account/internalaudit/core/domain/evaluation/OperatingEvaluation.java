package com.ho.account.internalaudit.core.domain.evaluation;

import lombok.Builder;
import java.util.List;

@Builder
public record OperatingEvaluation(
        String evaluationId,
        String controlId,
        String evaluatorId,
        String evaluationDate,
        Integer sampleSize,
        Integer exceptionCount,
        List<String> evidenceFilePaths,
        String result, // EFFECTIVE, INEFFECTIVE
        String remarks
) {}
