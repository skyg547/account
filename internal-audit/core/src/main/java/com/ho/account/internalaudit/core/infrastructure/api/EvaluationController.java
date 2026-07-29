package com.ho.account.internalaudit.core.infrastructure.api;

import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internalaudit/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationUseCase evaluationUseCase;

    @PostMapping("/design")
    public ResponseEntity<DesignEvaluation> submitDesignEvaluation(@RequestBody DesignEvaluation command) {
        return ResponseEntity.ok(evaluationUseCase.submitDesignEvaluation(command));
    }

    @PostMapping("/operating")
    public ResponseEntity<OperatingEvaluation> submitOperatingEvaluation(@RequestBody OperatingEvaluation command) {
        return ResponseEntity.ok(evaluationUseCase.submitOperatingEvaluation(command));
    }

    @PostMapping("/deficiencies")
    public ResponseEntity<Deficiency> registerDeficiency(@RequestBody Deficiency command) {
        return ResponseEntity.ok(evaluationUseCase.registerDeficiency(command));
    }

    @GetMapping("/controls/{controlId}/design")
    public ResponseEntity<List<DesignEvaluation>> getDesignEvaluations(@PathVariable String controlId) {
        return ResponseEntity.ok(evaluationUseCase.getDesignEvaluationsByControl(controlId));
    }

    @GetMapping("/controls/{controlId}/operating")
    public ResponseEntity<List<OperatingEvaluation>> getOperatingEvaluations(@PathVariable String controlId) {
        return ResponseEntity.ok(evaluationUseCase.getOperatingEvaluationsByControl(controlId));
    }
}
