package com.ho.account.audit.application.port.out;

import com.ho.account.audit.domain.MasterApproval;

public interface MasterDataChangeApplyPort {

    boolean supports(String masterType);

    void applyApprovedChange(MasterApproval approval);
}

