package com.ho.account.shared.infrastructure.security.application.port.out;

import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import java.util.List;
import java.util.Optional;

public interface MasterApprovalPersistencePort {

    MasterApproval save(MasterApproval approval);

    Optional<MasterApproval> findById(Long id);

    /** Implementations used for decisions must hold a write lock until the caller commits. */
    default Optional<MasterApproval> findByIdForDecision(Long id) {
        throw new UnsupportedOperationException("Decision locking is required.");
    }

    List<MasterApproval> findByStatus(MasterApproval.ApprovalStatus status);
}

