package com.ho.account.internalaudit.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.internalaudit.core.application.port.in.RcmUseCase;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RcmControllerTest {

    private RcmUseCase useCase;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        useCase = mock(RcmUseCase.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RcmController(useCase))
                .setControllerAdvice(new InternalAuditApiExceptionHandler())
                .build();
    }

    @Test
    void missingAuthUserReturnsUnauthorizedOnCreateProcess() throws Exception {
        mockMvc.perform(post("/api/v1/internalaudit/rcms/processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processId\":\"proc-1\",\"processName\":\"General Ledger\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    void blankAuthUserReturnsUnauthorizedOnCreateProcess() throws Exception {
        mockMvc.perform(post("/api/v1/internalaudit/rcms/processes")
                        .header(RcmController.AUTH_USER_HEADER, "   ")
                        .header(RcmController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processId\":\"proc-1\",\"processName\":\"General Ledger\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    void unauthorizedRoleReturnsForbiddenOnCreateProcess() throws Exception {
        mockMvc.perform(post("/api/v1/internalaudit/rcms/processes")
                        .header(RcmController.AUTH_USER_HEADER, "auditor1")
                        .header(RcmController.AUTH_ROLES_HEADER, "ROLE_GUEST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processId\":\"proc-1\",\"processName\":\"General Ledger\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    void createProcessDerivesOwnerIdFromActorWhenOmitted() throws Exception {
        RcmProcess saved = new RcmProcess("proc-1", "General Ledger", "Desc", "trusted_auditor");
        when(useCase.createProcess(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/internalaudit/rcms/processes")
                        .requestAttr(InternalAuditIdentityFilter.PRINCIPAL_ATTRIBUTE, new InternalAuditPrincipal("trusted_auditor", List.of("ROLE_AUDITOR"), 1))
                        .header(RcmController.AUTH_USER_HEADER, "trusted_auditor")
                        .header(RcmController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processId\":\"proc-1\",\"processName\":\"General Ledger\",\"description\":\"Desc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processId").value("proc-1"))
                .andExpect(jsonPath("$.ownerId").value("trusted_auditor"));

        ArgumentCaptor<RcmProcess> captor = ArgumentCaptor.forClass(RcmProcess.class);
        verify(useCase).createProcess(captor.capture());
        assertThat(captor.getValue().ownerId()).isEqualTo("trusted_auditor");
    }

    @Test
    void addRiskWithValidAuth() throws Exception {
        RcmRisk saved = new RcmRisk("risk-1", "proc-1", "Risk of error", "HIGH", "LIKELY");
        when(useCase.addRisk(eq("proc-1"), any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/internalaudit/rcms/processes/proc-1/risks")
                        .requestAttr(InternalAuditIdentityFilter.PRINCIPAL_ATTRIBUTE, new InternalAuditPrincipal("trusted_auditor", List.of("ROLE_AUDITOR"), 1))
                        .header(RcmController.AUTH_USER_HEADER, "trusted_auditor")
                        .header(RcmController.AUTH_ROLES_HEADER, "ROLE_AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskId\":\"risk-1\",\"processId\":\"proc-1\",\"riskDescription\":\"Risk of error\",\"impactLevel\":\"HIGH\",\"likelihood\":\"LIKELY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskId").value("risk-1"));

        verify(useCase).addRisk(eq("proc-1"), any());
    }

    @Test
    void addControlDerivesOwnerIdWhenOmitted() throws Exception {
        ControlActivity saved = new ControlActivity(
                "ctrl-1", "risk-1", "Dual sign-off", "PREVENTIVE", "MANUAL", "DAILY", "trusted_admin");
        when(useCase.addControl(eq("risk-1"), any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/internalaudit/rcms/risks/risk-1/controls")
                        .requestAttr(InternalAuditIdentityFilter.PRINCIPAL_ATTRIBUTE, new InternalAuditPrincipal("trusted_admin", List.of("ROLE_ADMIN"), 1))
                        .header(RcmController.AUTH_USER_HEADER, "trusted_admin")
                        .header(RcmController.AUTH_ROLES_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"controlId\":\"ctrl-1\",\"riskId\":\"risk-1\",\"controlDescription\":\"Dual sign-off\",\"controlType\":\"PREVENTIVE\",\"executionMethod\":\"MANUAL\",\"frequency\":\"DAILY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controlId").value("ctrl-1"))
                .andExpect(jsonPath("$.ownerId").value("trusted_admin"));

        ArgumentCaptor<ControlActivity> captor = ArgumentCaptor.forClass(ControlActivity.class);
        verify(useCase).addControl(eq("risk-1"), captor.capture());
        assertThat(captor.getValue().ownerId()).isEqualTo("trusted_admin");
    }

    @Test
    void getAllProcessesWithValidAuth() throws Exception {
        RcmProcess proc = new RcmProcess("proc-1", "GL", "Desc", "auditor");
        when(useCase.getAllProcesses()).thenReturn(List.of(proc));

        mockMvc.perform(get("/api/v1/internalaudit/rcms/processes")
                        .requestAttr(InternalAuditIdentityFilter.PRINCIPAL_ATTRIBUTE, new InternalAuditPrincipal("auditor", List.of("ROLE_AUDITOR"), 1))
                        .header(RcmController.AUTH_USER_HEADER, "auditor")
                        .header(RcmController.AUTH_ROLES_HEADER, "ROLE_AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].processId").value("proc-1"));

        verify(useCase).getAllProcesses();
    }

    @Test
    void getRisksByProcessWithValidAuth() throws Exception {
        RcmRisk risk = new RcmRisk("risk-1", "proc-1", "Desc", "HIGH", "LIKELY");
        when(useCase.getRisksByProcess("proc-1")).thenReturn(List.of(risk));

        mockMvc.perform(get("/api/v1/internalaudit/rcms/processes/proc-1/risks")
                        .requestAttr(InternalAuditIdentityFilter.PRINCIPAL_ATTRIBUTE, new InternalAuditPrincipal("auditor", List.of("ROLE_AUDITOR"), 1))
                        .header(RcmController.AUTH_USER_HEADER, "auditor")
                        .header(RcmController.AUTH_ROLES_HEADER, "ROLE_AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].riskId").value("risk-1"));

        verify(useCase).getRisksByProcess("proc-1");
    }

    @Test
    void getControlsByRiskWithValidAuth() throws Exception {
        ControlActivity ctrl = new ControlActivity(
                "ctrl-1", "risk-1", "Desc", "PREVENTIVE", "MANUAL", "DAILY", "owner");
        when(useCase.getControlsByRisk("risk-1")).thenReturn(List.of(ctrl));

        mockMvc.perform(get("/api/v1/internalaudit/rcms/risks/risk-1/controls")
                        .requestAttr(InternalAuditIdentityFilter.PRINCIPAL_ATTRIBUTE, new InternalAuditPrincipal("auditor", List.of("ROLE_AUDITOR"), 1))
                        .header(RcmController.AUTH_USER_HEADER, "auditor")
                        .header(RcmController.AUTH_ROLES_HEADER, "ROLE_AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].controlId").value("ctrl-1"));

        verify(useCase).getControlsByRisk("risk-1");
    }

    @Test
    void missingAuthReturnsUnauthorizedOnGetProcesses() throws Exception {
        mockMvc.perform(get("/api/v1/internalaudit/rcms/processes"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }
}
