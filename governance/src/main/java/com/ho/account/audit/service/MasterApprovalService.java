package com.ho.account.audit.service;

import com.ho.account.audit.application.port.in.MasterApprovalUseCase;
import com.ho.account.audit.application.port.out.MasterApprovalPersistencePort;
import com.ho.account.audit.application.port.out.MasterDataChangeApplyPort;
import com.ho.account.audit.domain.MasterApproval;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MasterApprovalService implements MasterApprovalUseCase {

    private final MasterApprovalPersistencePort masterApprovalPersistencePort;
    private final List<MasterDataChangeApplyPort> masterDataChangeApplyPorts;

    @Override
    @Transactional
    public MasterApproval requestApproval(RequestApprovalCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request approval command is required.");
        }

        MasterApproval approval = new MasterApproval();
        approval.setMasterType(command.masterType());
        approval.setMasterKey(command.masterKey());
        approval.setRequestType(command.requestType());
        approval.setPayload(command.payload());
        approval.setRequestUser(command.requestUser());
        approval.setEffectiveDate(command.effectiveDate() != null ? command.effectiveDate() : LocalDate.now());
        approval.setRequestedVersion(resolveRequestedVersion(command.requestedVersion()));
        approval.setAuditUser(command.requestUser());
        return masterApprovalPersistencePort.save(approval);
    }

    @Override
    @Transactional
    public MasterApproval approve(ApproveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Approve command is required.");
        }
        MasterApproval approval = masterApprovalPersistencePort.findById(command.approvalId())
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found. ID: " + command.approvalId()));

        approval.approve(command.approverUser(), command.remarks(), LocalDateTime.now());
        masterDataChangeApplyPorts.stream()
                .filter(port -> port.supports(approval.getMasterType()))
                .findFirst()
                .ifPresent(port -> port.applyApprovedChange(approval));

        return masterApprovalPersistencePort.save(approval);
    }

    @Override
    @Transactional
    public MasterApproval reject(RejectCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Reject command is required.");
        }
        MasterApproval approval = masterApprovalPersistencePort.findById(command.approvalId())
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found. ID: " + command.approvalId()));
        approval.reject(command.approverUser(), command.remarks(), LocalDateTime.now());
        return masterApprovalPersistencePort.save(approval);
    }

    @Override
    public List<MasterApproval> getPendingRequests() {
        return masterApprovalPersistencePort.findByStatus(MasterApproval.ApprovalStatus.PENDING);
    }

    private Integer resolveRequestedVersion(Integer requestedVersion) {
        if (requestedVersion == null) {
            return 1;
        }
        if (requestedVersion < 1) {
            throw new IllegalArgumentException("Requested version must be 1 or greater.");
        }
        return requestedVersion;
    }
}
