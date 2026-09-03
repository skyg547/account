package com.ho.account.internalaudit.api.adapter.in.web;

import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/internalaudit/evaluations")
public class EvaluationController {

    public static final String AUTH_USER_HEADER = "X-Auth-User";
    public static final String AUTH_ROLES_HEADER = "X-Auth-Roles";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_AUDITOR", "ROLE_ADMIN");

    private final EvaluationUseCase evaluationUseCase;

    public EvaluationController(EvaluationUseCase evaluationUseCase) {
        this.evaluationUseCase = evaluationUseCase;
    }

    @PostMapping("/design")
    public ResponseEntity<DesignEvaluation> submitDesignEvaluation(
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody DesignEvaluation command) {
        String actor = requireAuthorizedAuditor(authUser, authRoles);
        try {
            AuditActorContext.setActor(actor);
            AuditActorContext.setCorrelationId(correlationId);
            AuditActorContext.setIdempotencyKey(idempotencyKey);
            DesignEvaluation toSubmit = new DesignEvaluation(
                    command.evaluationId(),
                    command.controlId(),
                    actor,
                    command.evaluationDate(),
                    command.result(),
                    command.remarks());
            return ResponseEntity.ok(evaluationUseCase.submitDesignEvaluation(toSubmit));
        } finally {
            AuditActorContext.clear();
        }
    }

    @PostMapping("/operating")
    public ResponseEntity<OperatingEvaluation> submitOperatingEvaluation(
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody OperatingEvaluation command) {
        String actor = requireAuthorizedAuditor(authUser, authRoles);
        try {
            AuditActorContext.setActor(actor);
            AuditActorContext.setCorrelationId(correlationId);
            AuditActorContext.setIdempotencyKey(idempotencyKey);
            OperatingEvaluation toSubmit = new OperatingEvaluation(
                    command.evaluationId(),
                    command.controlId(),
                    actor,
                    command.evaluationDate(),
                    command.sampleSize(),
                    command.exceptionCount(),
                    command.evidenceFilePaths(),
                    command.result(),
                    command.remarks());
            return ResponseEntity.ok(evaluationUseCase.submitOperatingEvaluation(toSubmit));
        } finally {
            AuditActorContext.clear();
        }
    }

    @PostMapping("/deficiencies")
    public ResponseEntity<Deficiency> registerDeficiency(
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody Deficiency command) {
        String actor = requireAuthorizedAuditor(authUser, authRoles);
        try {
            AuditActorContext.setActor(actor);
            AuditActorContext.setCorrelationId(correlationId);
            AuditActorContext.setIdempotencyKey(idempotencyKey);
            return ResponseEntity.ok(evaluationUseCase.registerDeficiency(command));
        } finally {
            AuditActorContext.clear();
        }
    }

    @GetMapping("/controls/{controlId}/design")
    public ResponseEntity<List<DesignEvaluation>> getDesignEvaluations(
            @PathVariable String controlId,
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles) {
        requireAuthorizedAuditor(authUser, authRoles);
        return ResponseEntity.ok(evaluationUseCase.getDesignEvaluationsByControl(controlId));
    }

    @GetMapping("/controls/{controlId}/operating")
    public ResponseEntity<List<OperatingEvaluation>> getOperatingEvaluations(
            @PathVariable String controlId,
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles) {
        requireAuthorizedAuditor(authUser, authRoles);
        return ResponseEntity.ok(evaluationUseCase.getOperatingEvaluationsByControl(controlId));
    }

    private String requireAuthorizedAuditor(String authUser, String authRoles) {
        if (authUser == null || authUser.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated actor is required.");
        }
        boolean authorized = authRoles != null && Arrays.stream(authRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .anyMatch(ALLOWED_ROLES::contains);
        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Auditor or admin role is required.");
        }
        return authUser.trim();
    }
}
