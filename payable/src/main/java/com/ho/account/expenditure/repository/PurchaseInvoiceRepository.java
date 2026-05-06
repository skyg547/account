package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {
    Optional<PurchaseInvoice> findByInvoiceNoAndVendorCode(String invoiceNo, String vendorCode);
    List<PurchaseInvoice> findByVendorCodeAndStatus(String vendorCode, PurchaseInvoiceStatus status);
    List<PurchaseInvoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);
}
