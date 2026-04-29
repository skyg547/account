package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.PurchaseInvoice;
import java.util.Optional;

public interface PurchaseInvoicePersistencePort {
    PurchaseInvoice save(PurchaseInvoice invoice);
    Optional<PurchaseInvoice> findByInvoiceNoAndVendorCode(String invoiceNo, String vendorCode);
}
