package com.ho.account.income.repository;

import com.ho.account.income.domain.Receivable;
import com.ho.account.income.domain.ReceivableStatus;
import com.ho.account.income.domain.SalesInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, Long> {
    Optional<Receivable> findBySalesInvoice(SalesInvoice salesInvoice);
    List<Receivable> findByCustomerBusinessPartnerCodeAndStatus(String customerCode, ReceivableStatus status);
    List<Receivable> findByDueDateBeforeAndStatusNot(LocalDate dueDate, ReceivableStatus status);
    List<Receivable> findByCustomerBusinessPartnerCodeAndOutstandingAmountGreaterThan(String customerCode, BigDecimal amount);

    // For matching
    List<Receivable> findByCustomerBusinessPartnerCodeAndOutstandingAmountBetween(String customerCode, BigDecimal minAmount, BigDecimal maxAmount);
}
