package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayableRepository extends JpaRepository<Payable, Long> {
    Optional<Payable> findByPurchaseInvoiceNoAndPurchaseInvoiceVendorCode(String purchaseInvoiceNo,
            String purchaseInvoiceVendorCode);
    List<Payable> findByVendorCodeAndStatus(String vendorCode, PayableStatus status);
    List<Payable> findByDueDateBeforeAndStatusNot(LocalDate dueDate, PayableStatus status);
    List<Payable> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount);
}
