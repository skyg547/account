package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentRun;
import java.time.LocalDate;
import java.util.List;

public interface PaymentUseCase {
    PaymentRun initiatePaymentRun(PaymentRunCommand command);
    PaymentRun createPaymentRun(PaymentRunCommand command);
    void processPaymentRunChunk(Long paymentRunId, LocalDate runDate, List<Long> payableIds);
    void completePaymentRun(Long paymentRunId);
    Payment executePayment(ExecutePaymentCommand command);
    AdvancePayment recordAdvancePayment(AdvancePaymentCommand command);
    Payable offsetPayableWithAdvancePayment(OffsetPayableCommand command);
}