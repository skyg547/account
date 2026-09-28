package com.ho.account.closing.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.closing.application.port.in.AnnualClosingUseCase;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Issue #262: 서비스 인증 회계기간 상태 변경 경증 및 Reopen Approval 워크플로 통합 테스트.
 */
class ClosingFiscalPeriodControlBoundaryIntegrationTest {

    private static final String TRUSTED_ACTOR = "gateway-operator";

    private MockMvc mockMvc;
    private ClosingUseCase closingUseCase;
    private AnnualClosingUseCase annualClosingUseCase;

    @BeforeEach
    void setUp() {
        closingUseCase = mock(ClosingUseCase.class);
        annualClosingUseCase = mock(AnnualClosingUseCase.class);
        ClosingController controller = new ClosingController(closingUseCase, annualClosingUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ClosingExceptionHandler())
                .build();
    }

    @Test
    void requestPeriodReopenReturnsCreatedWhenValid() throws Exception {
        ReopenApproval approval = new ReopenApproval();
        approval.setId(10L);
        approval.assignFiscalPeriod(1L, "2026", "01");
        approval.request(TRUSTED_ACTOR, "Auditing adjustment needed");

        when(closingUseCase.requestPeriodReopen(1L, TRUSTED_ACTOR, "Auditing adjustment needed"))
                .thenReturn(approval);

        mockMvc.perform(post("/api/closing/reopen-approvals")
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fiscalPeriodId\":1,\"requestedBy\":\"USER_A\","
                                + "\"reason\":\"Auditing adjustment needed\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.requestedBy").value(TRUSTED_ACTOR));
    }

    @Test
    void requestPeriodReopenReturnsConflictWhenPendingRequestAlreadyExists() throws Exception {
        when(closingUseCase.requestPeriodReopen(1L, TRUSTED_ACTOR, "Duplicate request"))
                .thenThrow(new IllegalStateException("A pending reopen request already exists for the fiscal period."));

        mockMvc.perform(post("/api/closing/reopen-approvals")
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fiscalPeriodId\":1,\"requestedBy\":\"USER_A\","
                                + "\"reason\":\"Duplicate request\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_STATE_CONFLICT"));
    }

    @Test
    void approveReopenReturnsConflictWhenRequesterIsSameAsApprover() throws Exception {
        when(closingUseCase.updateReopenApprovalStatus(10L, ReopenApprovalStatus.APPROVED, TRUSTED_ACTOR))
                .thenThrow(new IllegalStateException("Reopen requester cannot approve or reject their own request."));

        mockMvc.perform(put("/api/closing/reopen-approvals/10/status")
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"approvedBy\":\"USER_A\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_STATE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Reopen requester cannot approve or reject their own request."));
    }

    @Test
    void approveReopenSucceedsWhenApproverIsDifferentFromRequester() throws Exception {
        ReopenApproval approval = new ReopenApproval();
        approval.setId(10L);
        approval.assignFiscalPeriod(1L, "2026", "01");
        approval.request("USER_A", "Auditing adjustment needed");
        approval.approve(TRUSTED_ACTOR);

        when(closingUseCase.updateReopenApprovalStatus(10L, ReopenApprovalStatus.APPROVED, TRUSTED_ACTOR))
                .thenReturn(approval);

        mockMvc.perform(put("/api/closing/reopen-approvals/10/status")
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"approvedBy\":\"USER_B_MANAGER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.approvedBy").value(TRUSTED_ACTOR));

        verify(closingUseCase).updateReopenApprovalStatus(10L, ReopenApprovalStatus.APPROVED, TRUSTED_ACTOR);
    }

    @Test
    void returnsNotFoundWhenFiscalPeriodOrApprovalDoesNotExist() throws Exception {
        when(closingUseCase.updateReopenApprovalStatus(999L, ReopenApprovalStatus.APPROVED, TRUSTED_ACTOR))
                .thenThrow(new EntityNotFoundException("ReopenApproval not found"));

        mockMvc.perform(put("/api/closing/reopen-approvals/999/status")
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"approvedBy\":\"USER_B\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
