package com.ho.account.audit.infrastructure.masterdata;

import com.ho.account.audit.application.port.out.MasterDataChangeApplyPort;
import com.ho.account.audit.domain.MasterApproval;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MasterDataChangeRequestAdapter implements MasterDataChangeApplyPort {

    private static final EnumSet<MasterDataChangeRequest.MasterDataType> SUPPORTED_TYPES = EnumSet.of(
            MasterDataChangeRequest.MasterDataType.ACCOUNT_SUBJECT,
            MasterDataChangeRequest.MasterDataType.BUSINESS_PARTNER,
            MasterDataChangeRequest.MasterDataType.DEPARTMENT,
            MasterDataChangeRequest.MasterDataType.PRODUCT);

    private final MasterDataChangeRequestUseCase masterDataChangeRequestUseCase;
    private final Clock clock;

    @Autowired
    public MasterDataChangeRequestAdapter(MasterDataChangeRequestUseCase masterDataChangeRequestUseCase) {
        this(masterDataChangeRequestUseCase, Clock.systemDefaultZone());
    }

    MasterDataChangeRequestAdapter(
            MasterDataChangeRequestUseCase masterDataChangeRequestUseCase,
            Clock clock) {
        this.masterDataChangeRequestUseCase = Objects.requireNonNull(
                masterDataChangeRequestUseCase, "masterDataChangeRequestUseCase is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public boolean supports(String masterType) {
        try {
            MasterDataChangeRequest.MasterDataType targetType =
                    MasterDataChangeRequest.MasterDataType.valueOf(masterType.trim().toUpperCase());
            return SUPPORTED_TYPES.contains(targetType);
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
                approval.getPayload(),
                sourceReference(approval));

        MasterDataChangeRequest current = masterDataChangeRequestUseCase.requestChange(command);
        if (current.getStatus() == ChangeStatus.APPLIED) {
            return;
        }
        if (current.getStatus() == ChangeStatus.REJECTED) {
            throw new IllegalStateException(
                    "Rejected master-data request cannot be retried. ID: " + current.getId());
        }
        if (current.getStatus() == ChangeStatus.REQUESTED) {
            current = masterDataChangeRequestUseCase.approve(current.getId(), approval.getApproverUser());
        }
        if (current.getStatus() != ChangeStatus.APPROVED) {
            throw new IllegalStateException(
                    "Master-data request is not approved after governance approval. ID: " + current.getId());
        }
        if (current.isReadyToApply(LocalDate.now(clock))) {
            masterDataChangeRequestUseCase.applyApprovedChange(current.getId());
        }
    }

    private LocalDate resolveEffectiveDate(MasterApproval approval) {
        if (approval.getEffectiveDate() != null) {
            return approval.getEffectiveDate();
        }
        if (approval.getRequestDate() != null) {
            return approval.getRequestDate().toLocalDate();
        }
        return LocalDate.now(clock);
    }

    private Integer resolveRequestedVersion(MasterApproval approval) {
        Integer requestedVersion = approval.getRequestedVersion();
        if (requestedVersion == null) {
            if (approval.getRequestType() == MasterApproval.ChangeRequestType.CREATE) {
                return 1;
            }
            throw new IllegalArgumentException(
                    "Requested version is required for UPDATE and DELETE master-data approvals.");
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

    private String sourceReference(MasterApproval approval) {
        if (approval.getId() == null) {
            throw new IllegalArgumentException("Persisted governance approval ID is required.");
        }
        return "governance-approval-id=" + approval.getId();
    }
}
