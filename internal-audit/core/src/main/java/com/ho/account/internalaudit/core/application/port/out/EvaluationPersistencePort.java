package com.ho.account.internalaudit.core.application.port.out;

import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;

public interface EvaluationPersistencePort {
    DesignEvaluation saveDesignEvaluation(DesignEvaluation evaluation);
    OperatingEvaluation saveOperatingEvaluation(OperatingEvaluation evaluation);
    Deficiency saveDeficiency(Deficiency deficiency);

    boolean controlActivityExists(String controlId);
    boolean evaluationExists(String evaluationId);
    List<DesignEvaluation> findDesignEvaluationsByControlId(String controlId);
    List<OperatingEvaluation> findOperatingEvaluationsByControlId(String controlId);
}
