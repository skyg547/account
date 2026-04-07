package com.ho.account.audit.service;

import com.ho.account.audit.domain.MasterApproval;
import com.ho.account.audit.repository.MasterApprovalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MasterApprovalService {

    @Autowired
    private MasterApprovalRepository masterApprovalRepository;

    @Transactional
    public MasterApproval requestApproval(String masterType, String masterKey,
            MasterApproval.ChangeRequestType requestType,
            String payload, String requestUser) {
        MasterApproval approval = new MasterApproval();
        approval.setMasterType(masterType);
        approval.setMasterKey(masterKey);
        approval.setRequestType(requestType);
        approval.setPayload(payload);
        approval.setRequestUser(requestUser);
        approval.setAuditUser(requestUser);
        return masterApprovalRepository.save(approval);
    }

    @Transactional
    public MasterApproval approve(Long id, String approverUser, String remarks) {
        MasterApproval approval = masterApprovalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval request not found"));

        if (approval.getRequestUser().equals(approverUser)) {
            throw new RuntimeException("Self-approval is not allowed (SOD Violation)");
        }

        approval.setStatus(MasterApproval.ApprovalStatus.APPROVED);
        approval.setApproverUser(approverUser);
        approval.setApprovalDate(LocalDateTime.now());
        approval.setRemarks(remarks);
        approval.setAuditUser(approverUser);

        // TODO: Trigger actual data change logic based on masterType and payload

        return masterApprovalRepository.save(approval);
    }

    @Transactional
    public MasterApproval reject(Long id, String approverUser, String remarks) {
        MasterApproval approval = masterApprovalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval request not found"));

        approval.setStatus(MasterApproval.ApprovalStatus.REJECTED);
        approval.setApproverUser(approverUser);
        approval.setApprovalDate(LocalDateTime.now());
        approval.setRemarks(remarks);
        approval.setAuditUser(approverUser);

        return masterApprovalRepository.save(approval);
    }

    public List<MasterApproval> getPendingRequests() {
        return masterApprovalRepository.findByStatus(MasterApproval.ApprovalStatus.PENDING);
    }
}
