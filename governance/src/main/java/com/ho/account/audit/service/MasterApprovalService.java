package com.ho.account.audit.service;

import com.ho.account.audit.application.port.in.MasterApprovalUseCase;
import com.ho.account.audit.application.port.out.MasterApprovalPersistencePort;
import com.ho.account.audit.application.port.out.MasterDataChangeApplyPort;
import com.ho.account.audit.domain.MasterApproval;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MasterApprovalService implements MasterApprovalUseCase {

    private final MasterApprovalPersistencePort masterApprovalPersistencePort;
    private final MasterDataChangeApplyPort masterDataChangeApplyPort;

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
        masterDataChangeApplyPort.applyApprovedChange(approval);

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
}
