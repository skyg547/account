package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.ReceivableStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.ReceivableJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.entity.SalesInvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableRepository extends JpaRepository<ReceivableJpaEntity, Long> {
    Optional<ReceivableJpaEntity> findBySalesInvoice(SalesInvoiceJpaEntity salesInvoice);
    List<ReceivableJpaEntity> findByCustomerCodeAndStatus(String customerCode, ReceivableStatus status);
    List<ReceivableJpaEntity> findByDueDateBeforeAndStatusNot(LocalDate dueDate, ReceivableStatus status);
    List<ReceivableJpaEntity> findByCustomerCodeAndOutstandingAmountGreaterThan(String customerCode, BigDecimal amount);
    List<ReceivableJpaEntity> findByCustomerCodeAndStatusIn(String customerCode, List<ReceivableStatus> statuses);

    // For matching
    List<ReceivableJpaEntity> findByCustomerCodeAndOutstandingAmountBetween(String customerCode, BigDecimal minAmount, BigDecimal maxAmount);
}
