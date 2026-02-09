package com.ho.account.tax.repository;

import com.ho.account.tax.domain.TaxInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {
    List<TaxInvoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);
}
