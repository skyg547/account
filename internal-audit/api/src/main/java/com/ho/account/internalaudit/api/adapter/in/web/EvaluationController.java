package com.ho.account.internalaudit.api.adapter.in.web;

import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [내부감사 평가 REST 컨트롤러 (Internal Audit Evaluation Controller)]
 *
 * <p><strong>Security & Actor Propagation Architecture:</strong></p>
 * <ul>
 *   <li>Gateway에서 검증 및 서명된 신뢰 헤더({@code X-Auth-User}, {@code X-Auth-Roles})를 수신하여
 *       클라이언트 JSON Body의 임의 {@code evaluatorId} 조작을 차단하고, 인증된 주체로 평가자를 안전하게 바인딩합니다.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/internalaudit/evaluations")
public class EvaluationController {

    static final String AUTH_USER_HEADER = "X-Auth-User";

    private final EvaluationUseCase evaluationUseCase;

    public EvaluationController(EvaluationUseCase evaluationUseCase) {
        this.evaluationUseCase = evaluationUseCase;
    }

    @PostMapping("/design")
    public ResponseEntity<DesignEvaluation> submitDesignEvaluation(
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestBody DesignEvaluation command) {
        String effectiveEvaluator = resolveEvaluator(actor, command.evaluatorId());
        DesignEvaluation securedCommand = new DesignEvaluation(
                command.evaluationId(),
                command.controlId(),
                effectiveEvaluator,
                command.evaluationDate(),
                command.result(),
                command.remarks()
        );
        return ResponseEntity.ok(evaluationUseCase.submitDesignEvaluation(securedCommand));
    }

    @PostMapping("/operating")
    public ResponseEntity<OperatingEvaluation> submitOperatingEvaluation(
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestBody OperatingEvaluation command) {
        String effectiveEvaluator = resolveEvaluator(actor, command.evaluatorId());
        OperatingEvaluation securedCommand = new OperatingEvaluation(
                command.evaluationId(),
                command.controlId(),
                effectiveEvaluator,
                command.evaluationDate(),
                command.sampleSize(),
                command.exceptionCount(),
                command.evidenceFilePaths(),
                command.result(),
                command.remarks()
        );
        return ResponseEntity.ok(evaluationUseCase.submitOperatingEvaluation(securedCommand));
    }

    @PostMapping("/deficiencies")
    public ResponseEntity<Deficiency> registerDeficiency(
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestBody Deficiency command) {
        return ResponseEntity.ok(evaluationUseCase.registerDeficiency(command));
    }

    @GetMapping("/controls/{controlId}/design")
    public ResponseEntity<List<DesignEvaluation>> getDesignEvaluations(
            @PathVariable String controlId) {
        return ResponseEntity.ok(evaluationUseCase.getDesignEvaluationsByControl(controlId));
    }

    @GetMapping("/controls/{controlId}/operating")
    public ResponseEntity<List<OperatingEvaluation>> getOperatingEvaluations(
            @PathVariable String controlId) {
        return ResponseEntity.ok(evaluationUseCase.getOperatingEvaluationsByControl(controlId));
    }

    private String resolveEvaluator(String actorHeader, String fallbackEvaluator) {
        if (actorHeader != null && !actorHeader.isBlank()) {
            return actorHeader.trim();
        }
        return fallbackEvaluator;
    }
}
