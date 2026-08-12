package com.ho.account.tax.repository;

import com.ho.account.tax.domain.TaxInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {
    List<TaxInvoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);
    Page<TaxInvoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    Optional<TaxInvoice> findByIssueId(String issueId);
}

