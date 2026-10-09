package com.ho.account.closing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.closing.application.port.in.ClosingTransitionRecoveryUseCase;
import com.ho.account.closing.application.service.ClosingTransitionPendingException;
import com.ho.account.closing.domain.ClosingCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ClosingTransitionControllerTest {
    private ClosingTransitionRecoveryUseCase recovery;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        recovery = mock(ClosingTransitionRecoveryUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new ClosingTransitionController(recovery))
                .setControllerAdvice(new ClosingExceptionHandler()).build();
    }

    @Test
    void recoveryRequiresTrustedActorAndClosingRole() throws Exception {
        mvc.perform(post("/api/closing/calendars/10/transition/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operationId\":\"op\",\"remoteRequestTerminated\":true}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/closing/calendars/10/transition/recover")
                .header("X-Auth-User", "operator").header("X-Auth-Roles", "AUDITOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operationId\":\"op\",\"remoteRequestTerminated\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/closing/calendars/10/transition")
                .header("X-Auth-User", "operator").header("X-Auth-Roles", "AUDITOR"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recovery);
    }

    @Test
    void recoveryUsesHeaderActorAndOriginalOperationId() throws Exception {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.OPEN);
        when(recovery.recoverTransition(10L, "operation-1", true, "trusted-checker")).thenReturn(calendar);
        mvc.perform(post("/api/closing/calendars/10/transition/recover")
                .header("X-Auth-User", "trusted-checker").header("X-Auth-Roles", "CLOSING_MANAGER")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operationId\":\"operation-1\",\"remoteRequestTerminated\":true,\"recoveredBy\":\"forged\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.calendarStatus").value("OPEN"));
        verify(recovery).recoverTransition(10L, "operation-1", true, "trusted-checker");
    }

    @Test
    void workflowConflictAndCommittedPendingTransitionHaveDifferentHttpResults() throws Exception {
        when(recovery.recoverTransition(anyLong(), any(), anyBoolean(), any()))
                .thenThrow(new IllegalStateException("original request termination must be confirmed"))
                .thenThrow(new ClosingTransitionPendingException("operation-1", new IllegalStateException("remote timeout")));
        mvc.perform(post("/api/closing/calendars/10/transition/recover")
                .header("X-Auth-User", "operator").header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON).content("{\"operationId\":\"operation-1\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("WORKFLOW_STATE_CONFLICT"));
        mvc.perform(post("/api/closing/calendars/10/transition/recover")
                .header("X-Auth-User", "operator").header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON).content("{\"operationId\":\"operation-1\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PERIOD_TRANSITION_RECOVERY_REQUIRED"));
    }

    @Test
    void cancellationRequiresTrustedClosingActorAndForwardsExactOperationAndReason() throws Exception {
        String path = "/api/closing/calendars/10/transition/cancel-prepared";
        String body = "{\"operationId\":\"operation-1\",\"reason\":\"expired snapshot\",\"actor\":\"forged\"}";
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("X-Auth-User", "operator").header("X-Auth-Roles", "AUDITOR")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recovery);

        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        when(recovery.cancelPreparedClose(10L, "operation-1", "expired snapshot", "trusted-operator"))
                .thenReturn(calendar);
        mvc.perform(post(path).header("X-Auth-User", "trusted-operator")
                .header("X-Auth-Roles", "CLOSING_MANAGER")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.calendarStatus").value("IN_PROGRESS"));
        verify(recovery).cancelPreparedClose(10L, "operation-1", "expired snapshot", "trusted-operator");
    }

    @Test
    void transitionQueryShowsBoundEvidenceIdToRecoveryOperator() throws Exception {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        calendar.prepareTransition("CLOSED", 20L, null, "bound-set-1", "closer");
        when(recovery.findTransition(10L)).thenReturn(calendar);

        mvc.perform(get("/api/closing/calendars/10/transition")
                .header("X-Auth-User", "operator").header("X-Auth-Roles", "CLOSING_MANAGER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evidenceSetId").value("bound-set-1"))
                .andExpect(jsonPath("$.operationId").value(calendar.getTransitionId()));
    }
}
