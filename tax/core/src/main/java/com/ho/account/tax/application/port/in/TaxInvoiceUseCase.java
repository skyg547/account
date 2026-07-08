package com.ho.account.tax.application.port.in;

import com.ho.account.tax.domain.TaxInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxInvoiceUseCase {
    TaxInvoice createAPInvoice(TaxInvoiceCommand command);
    Optional<TaxInvoice> getAPInvoiceById(Long id);
    Optional<TaxInvoice> getAPInvoiceByIssueId(String issueId);
    List<TaxInvoice> getAPInvoicesBetweenDates(LocalDate startDate, LocalDate endDate);
    TaxInvoice updateAPInvoice(Long id, TaxInvoiceCommand command);
    void cancelAPInvoice(Long id, String actor, String reason);
}