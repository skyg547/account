package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import java.util.Optional;

public interface ReopenApprovalPersistencePort {
    ReopenApproval save(ReopenApproval reopenApproval);
    ReopenApproval saveAndFlush(ReopenApproval reopenApproval);
    Optional<ReopenApproval> findById(Long id);
    boolean existsByFiscalPeriodIdAndStatus(Long fiscalPeriodId, ReopenApprovalStatus status);
}
