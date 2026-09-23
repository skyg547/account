package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.PurchaseInvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoiceJpaEntity, Long> {
    Optional<PurchaseInvoiceJpaEntity> findByInvoiceNoAndVendorCode(String invoiceNo, String vendorCode);
    List<PurchaseInvoiceJpaEntity> findByStatus(PurchaseInvoiceStatus status);
    List<PurchaseInvoiceJpaEntity> findByVendorCodeAndStatus(String vendorCode, PurchaseInvoiceStatus status);
    List<PurchaseInvoiceJpaEntity> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);
}
