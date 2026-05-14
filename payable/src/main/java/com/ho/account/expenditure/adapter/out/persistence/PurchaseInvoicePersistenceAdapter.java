package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.repository.PurchaseInvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * [PurchaseInvoicePersistenceAdapter]
 * 매입 인보이스 영속성 처리를 담당합니다.
 */
@Component
public class PurchaseInvoicePersistenceAdapter implements PurchaseInvoicePersistencePort {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;

    public PurchaseInvoicePersistenceAdapter(PurchaseInvoiceRepository purchaseInvoiceRepository) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
    }

    @Override
    public PurchaseInvoice save(PurchaseInvoice invoice) {
        return purchaseInvoiceRepository.save(invoice);
    }

    @Override
    public Optional<PurchaseInvoice> findByInvoiceNoAndVendorCode(String invoiceNo, String vendorCode) {
        return purchaseInvoiceRepository.findByInvoiceNoAndVendorCode(invoiceNo, vendorCode);
    }
}
