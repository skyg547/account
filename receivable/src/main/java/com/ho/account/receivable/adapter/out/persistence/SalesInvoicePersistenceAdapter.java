package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.repository.SalesInvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SalesInvoicePersistenceAdapter implements SalesInvoicePersistencePort {

    private final SalesInvoiceRepository salesInvoiceRepository;

    public SalesInvoicePersistenceAdapter(SalesInvoiceRepository salesInvoiceRepository) {
        this.salesInvoiceRepository = salesInvoiceRepository;
    }

    @Override
    public SalesInvoice save(SalesInvoice invoice) {
        return salesInvoiceRepository.save(invoice);
    }

    @Override
    public Optional<SalesInvoice> findById(Long id) {
        return salesInvoiceRepository.findById(id);
    }
}
