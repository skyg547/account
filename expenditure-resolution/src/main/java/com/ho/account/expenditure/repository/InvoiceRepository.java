package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.Invoice;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNo(String invoiceNo);

    List<Invoice> findByVendorAndStatus(BusinessPartner vendor, Invoice.InvoiceStatus status);
}
