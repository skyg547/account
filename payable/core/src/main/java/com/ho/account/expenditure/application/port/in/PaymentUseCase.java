package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentRun;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PaymentUseCase {
    PaymentRun initiatePaymentRun(LocalDate runDate, String description, String createdBy);
    Payment executePayment(Long paymentId, String bankAccount);
    AdvancePayment recordAdvancePayment(AdvancePayment advancePayment);
    Payable offsetPayableWithAdvancePayment(Long payableId, Long advancePaymentId, BigDecimal offsetAmount);
}
