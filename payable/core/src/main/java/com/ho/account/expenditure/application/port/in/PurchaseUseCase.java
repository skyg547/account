package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.PurchaseInvoice;
import java.time.LocalDate;

public interface PurchaseUseCase {
    PurchaseInvoice createPurchaseInvoice(PurchaseInvoiceCommand command);
    void updatePayableStatus(LocalDate asOfDate);
}