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
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.application.service.EvaluationService;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest
    @CsvSource(value = {
            "-1,-2", "-1,0", "0,-1", "-1,NULL", "NULL,-1", "10,11", "0,1",
            "-2147483648,NULL", "NULL,-2147483648", "2147483646,2147483647"
    }, nullValues = "NULL")
    void invalidOperatingCountsReturn400WithoutEvaluationOrAuditPersistence(
            Integer sampleSize, Integer exceptionCount) throws Exception {
        EvaluationPersistencePort evaluations = mock(EvaluationPersistencePort.class);
        AuditLogPersistencePort audits = mock(AuditLogPersistencePort.class);
        MockMvc realServiceMvc = operatingMvc(evaluations, audits);

        realServiceMvc.perform(post("/api/v1/internalaudit/evaluations/operating")
                        .header(EvaluationController.AUTH_USER_HEADER, "trusted_auditor")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operatingJson(sampleSize, exceptionCount)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(evaluations, audits);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "25,2", "10,10", "10,0", "0,0", "NULL,NULL", "0,NULL", "NULL,0",
            "10,NULL", "NULL,10", "2147483647,2147483647", "2147483647,0"
    }, nullValues = "NULL")
    void allowedOperatingCountsReachRealServiceUnchangedWithTrustedActor(
            Integer sampleSize, Integer exceptionCount) throws Exception {
        EvaluationPersistencePort evaluations = mock(EvaluationPersistencePort.class);
        AuditLogPersistencePort audits = mock(AuditLogPersistencePort.class);
        when(evaluations.controlActivityExists("ctrl-1")).thenReturn(true);
        when(evaluations.saveOperatingEvaluation(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMvc realServiceMvc = operatingMvc(evaluations, audits);

        String response = realServiceMvc.perform(post("/api/v1/internalaudit/evaluations/operating")
                        .header(EvaluationController.AUTH_USER_HEADER, " trusted_auditor ")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operatingJson(sampleSize, exceptionCount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluatorId").value("trusted_auditor"))
                .andExpect(jsonPath("$.result").value("EFFECTIVE"))
                .andReturn().getResponse().getContentAsString();

        ArgumentCaptor<OperatingEvaluation> saved = ArgumentCaptor.forClass(OperatingEvaluation.class);
        verify(evaluations).saveOperatingEvaluation(saved.capture());
        assertThat(saved.getValue().sampleSize()).isEqualTo(sampleSize);
        assertThat(saved.getValue().exceptionCount()).isEqualTo(exceptionCount);
        assertThat(saved.getValue().evaluatorId()).isEqualTo("trusted_auditor");
        assertThat(objectMapper.readValue(response, OperatingEvaluation.class)).isEqualTo(saved.getValue());
        ArgumentCaptor<AuditLogEntry> audit = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(audits).append(audit.capture());
        assertThat(audit.getValue().actor()).isEqualTo("trusted_auditor");
        assertThat(objectMapper.readValue(audit.getValue().detailsJson(), OperatingEvaluation.class))
                .isEqualTo(saved.getValue());
    }

    @Test
    void omittedOperatingCountsRemainUnspecifiedInSavedEvaluationAndAudit() throws Exception {
        EvaluationPersistencePort evaluations = mock(EvaluationPersistencePort.class);
        AuditLogPersistencePort audits = mock(AuditLogPersistencePort.class);
        when(evaluations.controlActivityExists("ctrl-1")).thenReturn(true);
        when(evaluations.saveOperatingEvaluation(any())).thenAnswer(inv -> inv.getArgument(0));

        String response = operatingMvc(evaluations, audits)
                .perform(post("/api/v1/internalaudit/evaluations/operating")
                        .header(EvaluationController.AUTH_USER_HEADER, "trusted_auditor")
                        .header(EvaluationController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationId":"op-omitted","controlId":"ctrl-1","result":"EFFECTIVE"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ArgumentCaptor<OperatingEvaluation> saved = ArgumentCaptor.forClass(OperatingEvaluation.class);
        verify(evaluations).saveOperatingEvaluation(saved.capture());
        assertThat(saved.getValue().sampleSize()).isNull();
        assertThat(saved.getValue().exceptionCount()).isNull();
        assertThat(objectMapper.readValue(response, OperatingEvaluation.class)).isEqualTo(saved.getValue());
        ArgumentCaptor<AuditLogEntry> audit = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(audits).append(audit.capture());
        assertThat(objectMapper.readValue(audit.getValue().detailsJson(), OperatingEvaluation.class))
                .isEqualTo(saved.getValue());
    }

    private MockMvc operatingMvc(EvaluationPersistencePort evaluations, AuditLogPersistencePort audits) {
        // Exercise Jackson, the controller and the real service; only outbound storage is replaced.
        return MockMvcBuilders.standaloneSetup(new EvaluationController(new EvaluationService(evaluations, audits)))
                .setControllerAdvice(new InternalAuditApiExceptionHandler())
                .build();
    }

    private String operatingJson(Integer sampleSize, Integer exceptionCount) {
        // Raw JSON lets invalid values reach Jackson without constructing a validated domain record first.
        return """
                {"evaluationId":"op-boundary","controlId":"ctrl-1","evaluatorId":"spoofed_actor",
                 "evaluationDate":"2026-09-11","sampleSize":%s,"exceptionCount":%s,"result":" effective "}
                """.formatted(sampleSize, exceptionCount);
    }

}
