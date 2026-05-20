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
    public boolean supports(String masterType) {
        try {
            MasterDataChangeRequest.MasterDataType.valueOf(masterType.trim().toUpperCase());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void applyApprovedChange(MasterApproval approval) {
        MasterDataChangeRequestCommand command = new MasterDataChangeRequestCommand(
                mapMasterType(approval.getMasterType()),
                approval.getMasterKey(),
                mapChangeType(approval.getRequestType()),
                resolveEffectiveDate(approval),
                resolveRequestedVersion(approval),
                approval.getRequestUser(),
                buildReason(approval),
                approval.getPayload());

        MasterDataChangeRequest requested = masterDataChangeRequestUseCase.requestChange(command);
        MasterDataChangeRequest approved =
                masterDataChangeRequestUseCase.approve(requested.getId(), approval.getApproverUser());
        masterDataChangeRequestUseCase.applyApprovedChange(approved.getId());
    }

    private LocalDate resolveEffectiveDate(MasterApproval approval) {
        if (approval.getEffectiveDate() != null) {
            return approval.getEffectiveDate();
        }
        if (approval.getRequestDate() != null) {
            return approval.getRequestDate().toLocalDate();
        }
        return LocalDate.now();
    }

    private Integer resolveRequestedVersion(MasterApproval approval) {
        Integer requestedVersion = approval.getRequestedVersion();
        if (requestedVersion == null) {
            return 1;
        }
        if (requestedVersion < 1) {
            throw new IllegalArgumentException("Requested version must be 1 or greater.");
        }
        return requestedVersion;
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
