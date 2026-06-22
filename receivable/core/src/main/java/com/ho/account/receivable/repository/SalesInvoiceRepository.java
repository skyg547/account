package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.domain.SalesInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {
    Optional<SalesInvoice> findByInvoiceNo(String invoiceNo);
    List<SalesInvoice> findByCustomerCodeAndStatus(String customerCode, SalesInvoiceStatus status);
    List<SalesInvoice> findByDueDateBeforeAndStatusIn(LocalDate dueDate, List<SalesInvoiceStatus> statuses);
}
