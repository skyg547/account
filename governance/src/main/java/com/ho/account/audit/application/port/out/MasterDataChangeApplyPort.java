package com.ho.account.audit.application.port.out;

import com.ho.account.audit.domain.MasterApproval;

public interface MasterDataChangeApplyPort {

    void applyApprovedChange(MasterApproval approval);
}

