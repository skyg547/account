package com.ho.account.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.audit.application.port.in.MasterApprovalUseCase;
import com.ho.account.audit.application.port.out.MasterApprovalPersistencePort;
import com.ho.account.audit.application.port.out.MasterDataChangeApplyPort;
import com.ho.account.audit.domain.MasterApproval;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MasterApprovalServiceTest {

    @Test
    void requestApproval_preservesEffectiveDateAndRequestedVersion() {
        MasterApprovalPersistencePort approvalPort = Mockito.mock(MasterApprovalPersistencePort.class);
        MasterDataChangeApplyPort applyPort = Mockito.mock(MasterDataChangeApplyPort.class);
        MasterApprovalService service = new MasterApprovalService(approvalPort, applyPort);
        when(approvalPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MasterApproval.class));

        MasterApproval approval = service.requestApproval(new MasterApprovalUseCase.RequestApprovalCommand(
                "DEPARTMENT",
                "DEP-01",
                MasterApproval.ChangeRequestType.UPDATE,
                "{\"name\":\"Finance\"}",
                "requester01",
                LocalDate.of(2026, 6, 1),
                7));

        assertThat(approval.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(approval.getRequestedVersion()).isEqualTo(7);
    }

    @Test
    void approve_appliesMasterDataChangeAndPersistsApproval() {
        MasterApprovalPersistencePort approvalPort = Mockito.mock(MasterApprovalPersistencePort.class);
        MasterDataChangeApplyPort applyPort = Mockito.mock(MasterDataChangeApplyPort.class);
        MasterApprovalService service = new MasterApprovalService(approvalPort, applyPort);

        MasterApproval approval = pendingApproval("requester01");
        when(approvalPort.findById(1L)).thenReturn(Optional.of(approval));
        when(approvalPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MasterApproval.class));

        MasterApproval approved = service.approve(new MasterApprovalUseCase.ApproveCommand(
                1L, "approver01", "ok"));

        assertThat(approved.getStatus()).isEqualTo(MasterApproval.ApprovalStatus.APPROVED);
        assertThat(approved.getApproverUser()).isEqualTo("approver01");
        verify(applyPort).applyApprovedChange(approval);
        verify(approvalPort).save(approval);
    }

    @Test
    void approve_throwsOnSelfApproval() {
        MasterApprovalPersistencePort approvalPort = Mockito.mock(MasterApprovalPersistencePort.class);
        MasterDataChangeApplyPort applyPort = Mockito.mock(MasterDataChangeApplyPort.class);
        MasterApprovalService service = new MasterApprovalService(approvalPort, applyPort);

        MasterApproval approval = pendingApproval("requester01");
        when(approvalPort.findById(1L)).thenReturn(Optional.of(approval));

        assertThatThrownBy(() -> service.approve(new MasterApprovalUseCase.ApproveCommand(
                1L, "requester01", "self approve")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Self-approval");
    }

    private MasterApproval pendingApproval(String requester) {
        MasterApproval approval = new MasterApproval();
        approval.setId(1L);
        approval.setMasterType("DEPARTMENT");
        approval.setMasterKey("DEP-01");
        approval.setRequestType(MasterApproval.ChangeRequestType.UPDATE);
        approval.setPayload("{\"name\":\"재무기획팀\"}");
        approval.setRequestUser(requester);
        approval.setStatus(MasterApproval.ApprovalStatus.PENDING);
        return approval;
    }
}
