package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.AdvancePayment;
import java.util.Optional;

public interface AdvancePaymentPersistencePort {
    AdvancePayment save(AdvancePayment advancePayment);
    Optional<AdvancePayment> findById(Long id);
}
