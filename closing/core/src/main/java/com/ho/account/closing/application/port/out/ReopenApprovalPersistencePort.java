package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ReopenApproval;
import java.util.Optional;

public interface ReopenApprovalPersistencePort {
    ReopenApproval save(ReopenApproval reopenApproval);
    Optional<ReopenApproval> findById(Long id);
}
