package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNo(String invoiceNo);

    List<Invoice> findByVendorCodeAndStatus(String vendorCode, Invoice.InvoiceStatus status);
}