package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.PurchaseInvoice;
import java.time.LocalDate;
import java.util.List;

public interface PurchaseUseCase {
    PurchaseInvoice createPurchaseInvoice(PurchaseInvoiceCommand command);
    List<PurchaseInvoice> findInvoices(String status);
    PurchaseInvoice findInvoiceById(Long id);
    void updatePayableStatus(LocalDate asOfDate);
}
