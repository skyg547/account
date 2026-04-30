package com.ho.account.tax.adapter.out.persistence;

import com.ho.account.tax.application.port.out.TaxInvoicePersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.repository.TaxInvoiceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class TaxInvoicePersistenceAdapter implements TaxInvoicePersistencePort {

    private final TaxInvoiceRepository taxInvoiceRepository;

    public TaxInvoicePersistenceAdapter(TaxInvoiceRepository taxInvoiceRepository) {
        this.taxInvoiceRepository = taxInvoiceRepository;
    }

    @Override
    public TaxInvoice save(TaxInvoice taxInvoice) {
        return taxInvoiceRepository.save(taxInvoice);
    }

    @Override
    public Optional<TaxInvoice> findById(Long id) {
        return taxInvoiceRepository.findById(id);
    }

    @Override
    public Optional<TaxInvoice> findByIssueId(String issueId) {
        return taxInvoiceRepository.findByIssueId(issueId);
    }

    @Override
    public List<TaxInvoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate) {
        return taxInvoiceRepository.findByIssueDateBetween(startDate, endDate);
    }

    @Override
    public void delete(TaxInvoice taxInvoice) {
        taxInvoiceRepository.delete(taxInvoice);
    }
}
