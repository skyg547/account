package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.APPaymentStatus;
import java.util.List;
import java.util.Optional;

public interface APPaymentUseCase {
    APPayment createAPPayment(APPaymentCommand command);
    Optional<APPayment> getAPPaymentById(Long id);
    List<APPayment> getAPPaymentsByExpenditureResolution(Long expenditureResolutionId);
    APPayment updateAPPayment(Long id, APPaymentCommand command);
    APPayment updateAPPaymentStatus(Long id, APPaymentStatus status);
    void deleteAPPayment(Long id);
}