package com.ho.account.audit.web.dto;

import com.ho.account.audit.domain.MasterApproval;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MasterApprovalResponse(
        Long id,
        String masterType,
        String masterKey,
        MasterApproval.ChangeRequestType requestType,
        String payload,
        String requestUser,
        LocalDateTime requestDate,
        String approverUser,
        LocalDateTime approvalDate,
        MasterApproval.ApprovalStatus status,
        LocalDate effectiveDate,
        Integer requestedVersion,
        String remarks,
        String auditUser) {

    public static MasterApprovalResponse from(MasterApproval approval) {
        return new MasterApprovalResponse(
                approval.getId(),
                approval.getMasterType(),
                approval.getMasterKey(),
                approval.getRequestType(),
                approval.getPayload(),
                approval.getRequestUser(),
                approval.getRequestDate(),
                approval.getApproverUser(),
                approval.getApprovalDate(),
                approval.getStatus(),
                approval.getEffectiveDate(),
                approval.getRequestedVersion(),
                approval.getRemarks(),
                approval.getAuditUser());
    }
}
