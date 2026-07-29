package com.ho.account.audit.application.port.out;

import com.ho.account.audit.domain.MasterApproval;
import java.util.List;
import java.util.Optional;

public interface MasterApprovalPersistencePort {

    MasterApproval save(MasterApproval approval);

    Optional<MasterApproval> findById(Long id);

    List<MasterApproval> findByStatus(MasterApproval.ApprovalStatus status);
}

