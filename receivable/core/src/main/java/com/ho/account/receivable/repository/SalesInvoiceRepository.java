package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.SalesInvoiceStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.SalesInvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoiceJpaEntity, Long> {
    Optional<SalesInvoiceJpaEntity> findByInvoiceNo(String invoiceNo);
    List<SalesInvoiceJpaEntity> findByCustomerCodeAndStatus(String customerCode, SalesInvoiceStatus status);
    List<SalesInvoiceJpaEntity> findByDueDateBeforeAndStatusIn(LocalDate dueDate, List<SalesInvoiceStatus> statuses);
}
