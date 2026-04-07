package com.ho.account.expenditure.repository;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {
    Optional<PurchaseInvoice> findByInvoiceNoAndVendor(String invoiceNo, BusinessPartner vendor);
    Optional<PurchaseInvoice> findByInvoiceNoAndVendorBusinessPartnerCode(String invoiceNo, String vendorCode);
    List<PurchaseInvoice> findByVendorBusinessPartnerCodeAndStatus(String vendorCode, PurchaseInvoiceStatus status);
    List<PurchaseInvoice> findByDueDateBeforeAndStatusIn(LocalDate dueDate, List<PurchaseInvoiceStatus> statuses);
}
