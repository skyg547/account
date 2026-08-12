package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayableRepository extends JpaRepository<PayableJpaEntity, Long> {
    Optional<PayableJpaEntity> findByPurchaseInvoiceNoAndPurchaseInvoiceVendorCode(String purchaseInvoiceNo,
            String purchaseInvoiceVendorCode);
    List<PayableJpaEntity> findByVendorCodeAndStatus(String vendorCode, PayableStatus status);
    List<PayableJpaEntity> findByDueDateBeforeAndStatusNot(LocalDate dueDate, PayableStatus status);
    List<PayableJpaEntity> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount);
}
