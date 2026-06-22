package com.ho.account.tax.adapter.out.external;

import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.tax.repository.TaxInvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TaxInvoiceQueryAdapter implements TaxInvoiceQueryPort {

    private final TaxInvoiceRepository taxInvoiceRepository;

    public TaxInvoiceQueryAdapter(TaxInvoiceRepository taxInvoiceRepository) {
        this.taxInvoiceRepository = taxInvoiceRepository;
    }

    @Override
    public Optional<TaxInvoiceRef> findById(Long taxInvoiceId) {
        return taxInvoiceRepository.findById(taxInvoiceId)
                .map(taxInvoice -> new TaxInvoiceRef(
                        taxInvoice.getId(),
                        taxInvoice.getIssueId(),
                        taxInvoice.getType(),
                        taxInvoice.getStatus().name()));
    }
}
