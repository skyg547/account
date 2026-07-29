package com.ho.account.shared.infrastructure.security.application.port.out;

import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import java.util.List;
import java.util.Optional;

public interface MasterApprovalPersistencePort {

    MasterApproval save(MasterApproval approval);

    Optional<MasterApproval> findById(Long id);

    List<MasterApproval> findByStatus(MasterApproval.ApprovalStatus status);
}

