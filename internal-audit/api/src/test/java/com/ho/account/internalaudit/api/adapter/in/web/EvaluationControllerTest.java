package com.ho.account.internalaudit.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

class EvaluationControllerTest {

    private EvaluationUseCase evaluationUseCase;
    private EvaluationController controller;

    @BeforeEach
    void setUp() {
        evaluationUseCase = mock(EvaluationUseCase.class);
        controller = new EvaluationController(evaluationUseCase);
    }

    @Test
    @DisplayName("X-Auth-User 헤더가 전달되면 클라이언트 body의 evaluatorId를 신뢰 헤더 주체로 덮어쓴다")
    void submitDesignEvaluationOverridesClientEvaluatorWithAuthUserHeader() {
        DesignEvaluation input = new DesignEvaluation(
                "EVAL-001",
                "CTRL-001",
                "attacker_evaluator",
                "2026-08-20",
                "EFFECTIVE",
                "Design test passed"
        );
        when(evaluationUseCase.submitDesignEvaluation(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<DesignEvaluation> response =
                controller.submitDesignEvaluation("auditor_kim", input);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ArgumentCaptor<DesignEvaluation> captor = ArgumentCaptor.forClass(DesignEvaluation.class);
        verify(evaluationUseCase).submitDesignEvaluation(captor.capture());

        assertThat(captor.getValue().evaluatorId()).isEqualTo("auditor_kim");
    }

    @Test
    @DisplayName("OperatingEvaluation 요청 시에도 X-Auth-User 헤더가 evaluatorId를 안전하게 주입한다")
    void submitOperatingEvaluationOverridesClientEvaluatorWithAuthUserHeader() {
        OperatingEvaluation input = new OperatingEvaluation(
                "EVAL-002",
                "CTRL-001",
                "forged_evaluator",
                "2026-08-20",
                25,
                0,
                List.of("/path/to/evidence.pdf"),
                "EFFECTIVE",
                "Operating sample test passed"
        );
        when(evaluationUseCase.submitOperatingEvaluation(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<OperatingEvaluation> response =
                controller.submitOperatingEvaluation("auditor_lee", input);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ArgumentCaptor<OperatingEvaluation> captor = ArgumentCaptor.forClass(OperatingEvaluation.class);
        verify(evaluationUseCase).submitOperatingEvaluation(captor.capture());

        assertThat(captor.getValue().evaluatorId()).isEqualTo("auditor_lee");
    }
}
