package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.APPaymentStatus;
import com.ho.account.expenditure.dto.APPaymentRequestDto;
import java.util.List;
import java.util.Optional;

public interface APPaymentUseCase {
    APPayment createAPPayment(APPaymentRequestDto requestDto);
    Optional<APPayment> getAPPaymentById(Long id);
    List<APPayment> getAPPaymentsByExpenditureResolution(Long expenditureResolutionId);
    APPayment updateAPPayment(Long id, APPaymentRequestDto requestDto);
    APPayment updateAPPaymentStatus(Long id, APPaymentStatus status);
    void deleteAPPayment(Long id);
}
