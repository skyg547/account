package com.ho.account.tax.application.port.out;

import com.ho.account.tax.domain.TaxInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxInvoicePersistencePort {
    TaxInvoice save(TaxInvoice taxInvoice);
    Optional<TaxInvoice> findById(Long id);
    Optional<TaxInvoice> findByIssueId(String issueId);
    List<TaxInvoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);
    void delete(TaxInvoice taxInvoice);
}
