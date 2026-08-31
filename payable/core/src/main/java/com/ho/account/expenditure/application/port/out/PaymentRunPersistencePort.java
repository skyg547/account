package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.PaymentRun;
import java.time.LocalDate;
import java.util.Optional;

public interface PaymentRunPersistencePort {
    PaymentRun save(PaymentRun paymentRun);
    Optional<PaymentRun> findById(Long id);
    Optional<PaymentRun> findByRunDateAndDescriptionAndCreatedBy(LocalDate runDate, String description, String createdBy);
}
