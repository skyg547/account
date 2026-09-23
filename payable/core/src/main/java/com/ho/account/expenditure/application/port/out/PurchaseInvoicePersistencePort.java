package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import java.util.List;
import java.util.Optional;

public interface PurchaseInvoicePersistencePort {
    PurchaseInvoice save(PurchaseInvoice invoice);
    List<PurchaseInvoice> findAll();
    List<PurchaseInvoice> findByStatus(PurchaseInvoiceStatus status);
    Optional<PurchaseInvoice> findById(Long id);
    Optional<PurchaseInvoice> findByInvoiceNoAndVendorCode(String invoiceNo, String vendorCode);
}
