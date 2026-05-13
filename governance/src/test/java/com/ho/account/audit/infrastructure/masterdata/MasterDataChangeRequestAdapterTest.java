package com.ho.account.audit.infrastructure.masterdata;

import com.ho.account.audit.domain.MasterApproval;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MasterDataChangeRequestAdapterTest {

    @Test
    void applyApprovedChangePreservesEffectiveDateAndRequestedVersion() {
        MasterDataChangeRequestUseCase useCase = mock(MasterDataChangeRequestUseCase.class);
        MasterDataChangeRequest requested = mock(MasterDataChangeRequest.class);
        MasterDataChangeRequest approved = mock(MasterDataChangeRequest.class);
        when(requested.getId()).thenReturn(101L);
        when(approved.getId()).thenReturn(102L);
        when(useCase.requestChange(org.mockito.ArgumentMatchers.any(MasterDataChangeRequestCommand.class)))
                .thenReturn(requested);
        when(useCase.approve(101L, "approver01")).thenReturn(approved);
        MasterDataChangeRequestAdapter adapter = new MasterDataChangeRequestAdapter(useCase);

        MasterApproval approval = new MasterApproval();
        approval.setId(55L);
        approval.setMasterType("DEPARTMENT");
        approval.setMasterKey("DEP-01");
        approval.setRequestType(MasterApproval.ChangeRequestType.UPDATE);
        approval.setPayload("{\"name\":\"Finance\"}");
        approval.setRequestUser("requester01");
        approval.setApproverUser("approver01");
        approval.setRemarks("approved");
        approval.setEffectiveDate(LocalDate.of(2026, 6, 1));
        approval.setRequestedVersion(7);

        adapter.applyApprovedChange(approval);

        ArgumentCaptor<MasterDataChangeRequestCommand> commandCaptor =
                ArgumentCaptor.forClass(MasterDataChangeRequestCommand.class);
        verify(useCase).requestChange(commandCaptor.capture());
        MasterDataChangeRequestCommand command = commandCaptor.getValue();
        assertThat(command.targetType()).isEqualTo(MasterDataChangeRequest.MasterDataType.DEPARTMENT);
        assertThat(command.targetKey()).isEqualTo("DEP-01");
        assertThat(command.changeType()).isEqualTo(MasterDataChangeRequest.ChangeType.UPDATE);
        assertThat(command.effectiveDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(command.requestedVersion()).isEqualTo(7);
        assertThat(command.requestedBy()).isEqualTo("requester01");
        assertThat(command.payloadJson()).isEqualTo("{\"name\":\"Finance\"}");
        assertThat(command.reason()).contains("approved");
        assertThat(command.reason()).contains("governance-approval-id=55");
        verify(useCase).approve(101L, "approver01");
        verify(useCase).applyApprovedChange(102L);
    }
}
