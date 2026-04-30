package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingAdjustment;

public interface ClosingAdjustmentPersistencePort {
    ClosingAdjustment save(ClosingAdjustment closingAdjustment);
}
