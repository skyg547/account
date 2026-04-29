package com.ho.account.audit.application.port.in;

import com.ho.account.audit.domain.MasterApproval;
import java.util.List;

public interface MasterApprovalUseCase {

    MasterApproval requestApproval(RequestApprovalCommand command);

    MasterApproval approve(ApproveCommand command);

    MasterApproval reject(RejectCommand command);

    List<MasterApproval> getPendingRequests();

    record RequestApprovalCommand(
            String masterType,
            String masterKey,
            MasterApproval.ChangeRequestType requestType,
            String payload,
            String requestUser) {
    }

    record ApproveCommand(
            Long approvalId,
            String approverUser,
            String remarks) {
    }

    record RejectCommand(
            Long approvalId,
            String approverUser,
            String remarks) {
    }
}

