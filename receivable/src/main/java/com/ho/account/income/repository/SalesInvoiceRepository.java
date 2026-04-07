package com.ho.account.income.repository;

import com.ho.account.income.domain.SalesInvoice;
import com.ho.account.income.domain.SalesInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {
    Optional<SalesInvoice> findByInvoiceNo(String invoiceNo);
    List<SalesInvoice> findByCustomerBusinessPartnerCodeAndStatus(String customerCode, SalesInvoiceStatus status);
    List<SalesInvoice> findByDueDateBeforeAndStatusIn(LocalDate dueDate, List<SalesInvoiceStatus> statuses);
}
