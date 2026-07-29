package com.ho.account.internalaudit.core.application.port.in;

import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;

public interface EvaluationUseCase {
    DesignEvaluation submitDesignEvaluation(DesignEvaluation command);
    OperatingEvaluation submitOperatingEvaluation(OperatingEvaluation command);
    Deficiency registerDeficiency(Deficiency command);

    List<DesignEvaluation> getDesignEvaluationsByControl(String controlId);
    List<OperatingEvaluation> getOperatingEvaluationsByControl(String controlId);
}
