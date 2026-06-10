package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.ReceivableStatus;
import com.ho.account.receivable.domain.SalesInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, Long> {
    Optional<Receivable> findBySalesInvoice(SalesInvoice salesInvoice);
    List<Receivable> findByCustomerCodeAndStatus(String customerCode, ReceivableStatus status);
    List<Receivable> findByDueDateBeforeAndStatusNot(LocalDate dueDate, ReceivableStatus status);
    List<Receivable> findByCustomerCodeAndOutstandingAmountGreaterThan(String customerCode, BigDecimal amount);
    List<Receivable> findByCustomerCodeAndStatusIn(String customerCode, List<ReceivableStatus> statuses);

    // For matching
    List<Receivable> findByCustomerCodeAndOutstandingAmountBetween(String customerCode, BigDecimal minAmount, BigDecimal maxAmount);
}
