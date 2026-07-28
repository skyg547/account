package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentRun;

public interface PaymentUseCase {
    PaymentRun initiatePaymentRun(PaymentRunCommand command);
    Payment executePayment(ExecutePaymentCommand command);
    AdvancePayment recordAdvancePayment(AdvancePaymentCommand command);
    Payable offsetPayableWithAdvancePayment(OffsetPayableCommand command);
}