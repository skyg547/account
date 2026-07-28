package com.ho.account.internalaudit.core.infrastructure.persistence.adapter;

import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.DeficiencyJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.DesignEvaluationJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.OperatingEvaluationJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.DeficiencyRepository;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.DesignEvaluationRepository;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.OperatingEvaluationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvaluationPersistenceAdapter implements EvaluationPersistencePort {

    private final DesignEvaluationRepository designEvaluationRepository;
    private final OperatingEvaluationRepository operatingEvaluationRepository;
    private final DeficiencyRepository deficiencyRepository;

    @Override
    public DesignEvaluation saveDesignEvaluation(DesignEvaluation evaluation) {
        DesignEvaluationJpaEntity entity = DesignEvaluationJpaEntity.builder()
                .evaluationId(evaluation.evaluationId())
                .controlId(evaluation.controlId())
                .evaluatorId(evaluation.evaluatorId())
                .evaluationDate(evaluation.evaluationDate())
                .result(evaluation.result())
                .remarks(evaluation.remarks())
                .build();
        DesignEvaluationJpaEntity saved = designEvaluationRepository.save(entity);
        return main(saved);
    }

    @Override
    public OperatingEvaluation saveOperatingEvaluation(OperatingEvaluation evaluation) {
        OperatingEvaluationJpaEntity entity = OperatingEvaluationJpaEntity.builder()
                .evaluationId(evaluation.evaluationId())
                .controlId(evaluation.controlId())
                .evaluatorId(evaluation.evaluatorId())
                .evaluationDate(evaluation.evaluationDate())
                .sampleSize(evaluation.sampleSize())
                .exceptionCount(evaluation.exceptionCount())
                .evidenceFilePaths(evaluation.evidenceFilePaths())
                .result(evaluation.result())
                .remarks(evaluation.remarks())
                .build();
        OperatingEvaluationJpaEntity saved = operatingEvaluationRepository.save(entity);
        return main(saved);
    }

    @Override
    public Deficiency saveDeficiency(Deficiency deficiency) {
        DeficiencyJpaEntity entity = DeficiencyJpaEntity.builder()
                .deficiencyId(deficiency.deficiencyId())
                .evaluationId(deficiency.evaluationId())
                .description(deficiency.description())
                .remediationPlan(deficiency.remediationPlan())
                .status(deficiency.status())
                .build();
        DeficiencyJpaEntity saved = deficiencyRepository.save(entity);
        return main(saved);
    }

    @Override
    public List<DesignEvaluation> findDesignEvaluationsByControlId(String controlId) {
        return designEvaluationRepository.findByControlId(controlId).stream().map(this::main).collect(Collectors.toList());
    }

    @Override
    public List<OperatingEvaluation> findOperatingEvaluationsByControlId(String controlId) {
        return operatingEvaluationRepository.findByControlId(controlId).stream().map(this::main).collect(Collectors.toList());
    }

    private DesignEvaluation main(DesignEvaluationJpaEntity entity) {
        return DesignEvaluation.builder()
                .evaluationId(entity.getEvaluationId())
                .controlId(entity.getControlId())
                .evaluatorId(entity.getEvaluatorId())
                .evaluationDate(entity.getEvaluationDate())
                .result(entity.getResult())
                .remarks(entity.getRemarks())
                .build();
    }

    private OperatingEvaluation main(OperatingEvaluationJpaEntity entity) {
        return OperatingEvaluation.builder()
                .evaluationId(entity.getEvaluationId())
                .controlId(entity.getControlId())
                .evaluatorId(entity.getEvaluatorId())
                .evaluationDate(entity.getEvaluationDate())
                .sampleSize(entity.getSampleSize())
                .exceptionCount(entity.getExceptionCount())
                .evidenceFilePaths(entity.getEvidenceFilePaths())
                .result(entity.getResult())
                .remarks(entity.getRemarks())
                .build();
    }

    private Deficiency main(DeficiencyJpaEntity entity) {
        return Deficiency.builder()
                .deficiencyId(entity.getDeficiencyId())
                .evaluationId(entity.getEvaluationId())
                .description(entity.getDescription())
                .remediationPlan(entity.getRemediationPlan())
                .status(entity.getStatus())
                .build();
    }
}
