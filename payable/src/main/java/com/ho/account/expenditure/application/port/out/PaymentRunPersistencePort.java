package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.PaymentRun;
import java.util.Optional;

public interface PaymentRunPersistencePort {
    PaymentRun save(PaymentRun paymentRun);
    Optional<PaymentRun> findById(Long id);
}
