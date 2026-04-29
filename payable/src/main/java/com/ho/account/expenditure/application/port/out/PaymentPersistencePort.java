package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.Payment;
import java.util.Optional;

public interface PaymentPersistencePort {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
}
