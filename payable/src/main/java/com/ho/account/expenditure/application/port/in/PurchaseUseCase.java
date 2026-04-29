package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.PurchaseInvoice;
import java.time.LocalDate;

public interface PurchaseUseCase {
    PurchaseInvoice createPurchaseInvoice(PurchaseInvoice invoice);
    void updatePayableStatus(LocalDate asOfDate);
}
