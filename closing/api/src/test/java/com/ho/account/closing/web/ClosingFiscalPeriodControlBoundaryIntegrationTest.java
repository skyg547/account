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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.closing.application.port.in.AnnualClosingUseCase;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.dto.ReopenApprovalRequestDto;
import com.ho.account.closing.dto.ReopenApprovalStatusUpdateDto;
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

    private MockMvc mockMvc;
    private ClosingUseCase closingUseCase;
    private AnnualClosingUseCase annualClosingUseCase;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        closingUseCase = mock(ClosingUseCase.class);
        annualClosingUseCase = mock(AnnualClosingUseCase.class);
        ClosingController controller = new ClosingController(closingUseCase, annualClosingUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ClosingExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void requestPeriodReopenReturnsCreatedWhenValid() throws Exception {
        ReopenApproval approval = new ReopenApproval();
        approval.setId(10L);
        approval.assignFiscalPeriod(1L, "2026", "01");
        approval.request("USER_A", "Auditing adjustment needed");

        when(closingUseCase.requestPeriodReopen(1L, "USER_A", "Auditing adjustment needed"))
                .thenReturn(approval);

        ReopenApprovalRequestDto requestDto = new ReopenApprovalRequestDto();
        requestDto.setFiscalPeriodId(1L);
        requestDto.setRequestedBy("USER_A");
        requestDto.setReason("Auditing adjustment needed");

        mockMvc.perform(post("/api/closing/reopen-approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.requestedBy").value("USER_A"));
    }

    @Test
    void requestPeriodReopenReturnsConflictWhenPendingRequestAlreadyExists() throws Exception {
        when(closingUseCase.requestPeriodReopen(1L, "USER_A", "Duplicate request"))
                .thenThrow(new IllegalStateException("A pending reopen request already exists for the fiscal period."));

        ReopenApprovalRequestDto requestDto = new ReopenApprovalRequestDto();
        requestDto.setFiscalPeriodId(1L);
        requestDto.setRequestedBy("USER_A");
        requestDto.setReason("Duplicate request");

        mockMvc.perform(post("/api/closing/reopen-approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_STATE_CONFLICT"));
    }

    @Test
    void approveReopenReturnsConflictWhenRequesterIsSameAsApprover() throws Exception {
        when(closingUseCase.updateReopenApprovalStatus(10L, ReopenApprovalStatus.APPROVED, "USER_A"))
                .thenThrow(new IllegalStateException("Reopen requester cannot approve or reject their own request."));

        ReopenApprovalStatusUpdateDto updateDto = new ReopenApprovalStatusUpdateDto();
        updateDto.setStatus(ReopenApprovalStatus.APPROVED);
        updateDto.setApprovedBy("USER_A");

        mockMvc.perform(put("/api/closing/reopen-approvals/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
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
        approval.approve("USER_B_MANAGER");

        when(closingUseCase.updateReopenApprovalStatus(10L, ReopenApprovalStatus.APPROVED, "USER_B_MANAGER"))
                .thenReturn(approval);

        ReopenApprovalStatusUpdateDto updateDto = new ReopenApprovalStatusUpdateDto();
        updateDto.setStatus(ReopenApprovalStatus.APPROVED);
        updateDto.setApprovedBy("USER_B_MANAGER");

        mockMvc.perform(put("/api/closing/reopen-approvals/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.approvedBy").value("USER_B_MANAGER"));

        verify(closingUseCase).updateReopenApprovalStatus(10L, ReopenApprovalStatus.APPROVED, "USER_B_MANAGER");
    }

    @Test
    void returnsNotFoundWhenFiscalPeriodOrApprovalDoesNotExist() throws Exception {
        when(closingUseCase.updateReopenApprovalStatus(999L, ReopenApprovalStatus.APPROVED, "USER_B"))
                .thenThrow(new EntityNotFoundException("ReopenApproval not found"));

        ReopenApprovalStatusUpdateDto updateDto = new ReopenApprovalStatusUpdateDto();
        updateDto.setStatus(ReopenApprovalStatus.APPROVED);
        updateDto.setApprovedBy("USER_B");

        mockMvc.perform(put("/api/closing/reopen-approvals/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
