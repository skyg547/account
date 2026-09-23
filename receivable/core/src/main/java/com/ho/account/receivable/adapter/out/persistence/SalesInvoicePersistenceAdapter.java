package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.domain.SalesInvoiceStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.SalesInvoiceJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.mapper.SalesInvoiceMapper;
import com.ho.account.receivable.repository.SalesInvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SalesInvoicePersistenceAdapter implements SalesInvoicePersistencePort {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceMapper salesInvoiceMapper;

    public SalesInvoicePersistenceAdapter(SalesInvoiceRepository salesInvoiceRepository,
                                           SalesInvoiceMapper salesInvoiceMapper) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.salesInvoiceMapper = salesInvoiceMapper;
    }

    @Override
    public SalesInvoice save(SalesInvoice invoice) {
        SalesInvoiceJpaEntity entity = salesInvoiceMapper.toEntity(invoice);
        SalesInvoiceJpaEntity savedEntity = salesInvoiceRepository.save(entity);
        return salesInvoiceMapper.toDomain(savedEntity);
    }

    @Override
    public List<SalesInvoice> findAll() {
        return salesInvoiceRepository.findAll().stream()
                .map(salesInvoiceMapper::toDomain)
                .toList();
    }

    @Override
    public List<SalesInvoice> findByStatus(SalesInvoiceStatus status) {
        return salesInvoiceRepository.findByStatus(status).stream()
                .map(salesInvoiceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<SalesInvoice> findById(Long id) {
        return salesInvoiceRepository.findById(id)
                .map(salesInvoiceMapper::toDomain);
    }
}
