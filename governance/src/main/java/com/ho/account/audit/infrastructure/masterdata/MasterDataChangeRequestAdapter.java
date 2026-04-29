package com.ho.account.audit.infrastructure.masterdata;

import com.ho.account.audit.application.port.out.MasterDataChangeApplyPort;
import com.ho.account.audit.domain.MasterApproval;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MasterDataChangeRequestAdapter implements MasterDataChangeApplyPort {

    private final MasterDataChangeRequestUseCase masterDataChangeRequestUseCase;

    @Override
    public void applyApprovedChange(MasterApproval approval) {
        MasterDataChangeRequestCommand command = new MasterDataChangeRequestCommand(
                mapMasterType(approval.getMasterType()),
                approval.getMasterKey(),
                mapChangeType(approval.getRequestType()),
                LocalDate.now(),
                1,
                approval.getRequestUser(),
                buildReason(approval),
                approval.getPayload());

        MasterDataChangeRequest requested = masterDataChangeRequestUseCase.requestChange(command);
        MasterDataChangeRequest approved =
                masterDataChangeRequestUseCase.approve(requested.getId(), approval.getApproverUser());
        masterDataChangeRequestUseCase.applyApprovedChange(approved.getId());
    }

    private MasterDataChangeRequest.MasterDataType mapMasterType(String masterType) {
        try {
            return MasterDataChangeRequest.MasterDataType.valueOf(masterType.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported master type for change request: " + masterType, e);
        }
    }

    private MasterDataChangeRequest.ChangeType mapChangeType(MasterApproval.ChangeRequestType requestType) {
        return switch (requestType) {
            case CREATE -> MasterDataChangeRequest.ChangeType.CREATE;
            case UPDATE -> MasterDataChangeRequest.ChangeType.UPDATE;
            case DELETE -> MasterDataChangeRequest.ChangeType.DEACTIVATE;
        };
    }

    private String buildReason(MasterApproval approval) {
        String baseReason = approval.getRemarks() != null ? approval.getRemarks().trim() : "";
        String governanceTrace = "[governance-approval-id=" + approval.getId() + "]";
        if (baseReason.isBlank()) {
            return governanceTrace;
        }
        return baseReason + " " + governanceTrace;
    }
}

