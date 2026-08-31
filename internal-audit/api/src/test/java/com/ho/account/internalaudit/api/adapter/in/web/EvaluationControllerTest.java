package com.ho.account.internalaudit.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EvaluationControllerTest {

    private EvaluationUseCase useCase;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        useCase = mock(EvaluationUseCase.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EvaluationController(useCase))
                .setControllerAdvice(new InternalAuditApiExceptionHandler())
                .build();
    }

    @Test
    void missingAuthUserReturnsUnauthorizedOnDesignSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/internalaudit/evaluations/design")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationId\":\"eval-1\",\"controlId\":\"ctrl-1\",\"result\":\"EFFECTIVE\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    void blankAuthUserReturnsUnauthorizedOnDesignSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/internalaudit/evaluations/design")
                        .header(EvaluationController.AUTH_USER_HEADER, "   ")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationId\":\"eval-1\",\"controlId\":\"ctrl-1\",\"result\":\"EFFECTIVE\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    void unauthorizedRoleReturnsForbiddenOnDesignSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/internalaudit/evaluations/design")
                        .header(EvaluationController.AUTH_USER_HEADER, "auditor1")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationId\":\"eval-1\",\"controlId\":\"ctrl-1\",\"result\":\"EFFECTIVE\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    void missingRoleHeaderReturnsForbiddenOnDesignSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/internalaudit/evaluations/design")
                        .header(EvaluationController.AUTH_USER_HEADER, "auditor1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationId\":\"eval-1\",\"controlId\":\"ctrl-1\",\"result\":\"EFFECTIVE\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    void validAuditorHeaderOverridesEvaluatorIdAndSubmitsDesignEvaluation() throws Exception {
        DesignEvaluation saved = new DesignEvaluation(
                "eval-1", "ctrl-1", "trusted_auditor", "2026-08-31", "EFFECTIVE", "OK");
        when(useCase.submitDesignEvaluation(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/internalaudit/evaluations/design")
                        .header(EvaluationController.AUTH_USER_HEADER, "trusted_auditor")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationId\":\"eval-1\",\"controlId\":\"ctrl-1\",\"evaluatorId\":\"spoofed_actor\",\"evaluationDate\":\"2026-08-31\",\"result\":\"EFFECTIVE\",\"remarks\":\"OK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluatorId").value("trusted_auditor"))
                .andExpect(jsonPath("$.result").value("EFFECTIVE"));

        ArgumentCaptor<DesignEvaluation> captor = ArgumentCaptor.forClass(DesignEvaluation.class);
        verify(useCase).submitDesignEvaluation(captor.capture());
        assertThat(captor.getValue().evaluatorId()).isEqualTo("trusted_auditor");
    }

    @Test
    void validAdminHeaderOverridesEvaluatorIdAndSubmitsOperatingEvaluation() throws Exception {
        OperatingEvaluation saved = new OperatingEvaluation(
                "eval-op-1", "ctrl-1", "trusted_admin", "2026-08-31", 25, 0, List.of(), "EFFECTIVE", "OK");
        when(useCase.submitOperatingEvaluation(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/internalaudit/evaluations/operating")
                        .header(EvaluationController.AUTH_USER_HEADER, " trusted_admin ")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_ADMIN,ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationId\":\"eval-op-1\",\"controlId\":\"ctrl-1\",\"evaluatorId\":\"spoofed_actor\",\"evaluationDate\":\"2026-08-31\",\"sampleSize\":25,\"exceptionCount\":0,\"result\":\"EFFECTIVE\",\"remarks\":\"OK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluatorId").value("trusted_admin"))
                .andExpect(jsonPath("$.result").value("EFFECTIVE"));

        ArgumentCaptor<OperatingEvaluation> captor = ArgumentCaptor.forClass(OperatingEvaluation.class);
        verify(useCase).submitOperatingEvaluation(captor.capture());
        assertThat(captor.getValue().evaluatorId()).isEqualTo("trusted_admin");
    }

    @Test
    void invalidEvaluationResultReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/internalaudit/evaluations/design")
                        .header(EvaluationController.AUTH_USER_HEADER, "trusted_auditor")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationId\":\"eval-1\",\"controlId\":\"ctrl-1\",\"result\":\"INVALID_RESULT\"}"))
                .andExpect(status().isBadRequest());

        verify(useCase, never()).submitDesignEvaluation(any());
    }

    @Test
    void registerDeficiencyWithValidAuth() throws Exception {
        Deficiency saved = new Deficiency("def-1", "eval-1", "Gap found", "Fix policy", "OPEN");
        when(useCase.registerDeficiency(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/internalaudit/evaluations/deficiencies")
                        .header(EvaluationController.AUTH_USER_HEADER, "trusted_auditor")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deficiencyId").value("def-1"));

        verify(useCase).registerDeficiency(any());
    }

    @Test
    void missingAuthUserReturnsUnauthorizedOnGet() throws Exception {
        mockMvc.perform(get("/api/v1/internalaudit/evaluations/controls/ctrl-1/design"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    void validAuthReturnsDesignEvaluationsOnGet() throws Exception {
        DesignEvaluation eval = new DesignEvaluation(
                "eval-1", "ctrl-1", "auditor", "2026-08-31", "EFFECTIVE", "OK");
        when(useCase.getDesignEvaluationsByControl("ctrl-1")).thenReturn(List.of(eval));

        mockMvc.perform(get("/api/v1/internalaudit/evaluations/controls/ctrl-1/design")
                        .header(EvaluationController.AUTH_USER_HEADER, "auditor")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationId").value("eval-1"));

        verify(useCase).getDesignEvaluationsByControl("ctrl-1");
    }

    @Test
    void validAuthReturnsOperatingEvaluationsOnGet() throws Exception {
        OperatingEvaluation eval = new OperatingEvaluation(
                "eval-op-1", "ctrl-1", "auditor", "2026-08-31", 10, 0, List.of(), "EFFECTIVE", "OK");
        when(useCase.getOperatingEvaluationsByControl("ctrl-1")).thenReturn(List.of(eval));

        mockMvc.perform(get("/api/v1/internalaudit/evaluations/controls/ctrl-1/operating")
                        .header(EvaluationController.AUTH_USER_HEADER, "auditor")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationId").value("eval-op-1"));

        verify(useCase).getOperatingEvaluationsByControl("ctrl-1");
    }
}
