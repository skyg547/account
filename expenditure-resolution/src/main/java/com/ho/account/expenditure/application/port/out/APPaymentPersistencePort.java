package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.APPayment;
import java.util.List;
import java.util.Optional;

public interface APPaymentPersistencePort {
    APPayment save(APPayment apPayment);
    Optional<APPayment> findById(Long id);
    List<APPayment> findByExpenditureResolutionId(Long expenditureResolutionId);
    void delete(APPayment apPayment);
}
