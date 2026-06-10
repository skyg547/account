package com.ho.account.tax.application.port.in;

import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.dto.TaxInvoiceRequestDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxInvoiceUseCase {
    TaxInvoice createAPInvoice(TaxInvoiceRequestDto requestDto);
    Optional<TaxInvoice> getAPInvoiceById(Long id);
    Optional<TaxInvoice> getAPInvoiceByIssueId(String issueId);
    List<TaxInvoice> getAPInvoicesBetweenDates(LocalDate startDate, LocalDate endDate);
    TaxInvoice updateAPInvoice(Long id, TaxInvoiceRequestDto requestDto);
    void cancelAPInvoice(Long id, String actor, String reason);
}
