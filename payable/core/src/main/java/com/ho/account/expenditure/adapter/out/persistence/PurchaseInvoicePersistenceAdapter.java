package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.PurchaseInvoiceJpaEntity;
import com.ho.account.expenditure.infrastructure.persistence.mapper.PurchaseInvoiceMapper;
import com.ho.account.expenditure.repository.PurchaseInvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * [PurchaseInvoicePersistenceAdapter]
 * 매입 인보이스 영속성 처리를 담당하는 Persistence Adapter입니다.
 * PurchaseInvoiceMapper를 사용하여 Pure Java Domain POJO <-> JPA Entity 변환을 수행합니다.
 */
@Component
public class PurchaseInvoicePersistenceAdapter implements PurchaseInvoicePersistencePort {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final PurchaseInvoiceMapper purchaseInvoiceMapper;

    public PurchaseInvoicePersistenceAdapter(PurchaseInvoiceRepository purchaseInvoiceRepository,
                                             PurchaseInvoiceMapper purchaseInvoiceMapper) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.purchaseInvoiceMapper = purchaseInvoiceMapper;
    }

    @Override
    public PurchaseInvoice save(PurchaseInvoice invoice) {
        PurchaseInvoiceJpaEntity entity = purchaseInvoiceMapper.toEntity(invoice);
        PurchaseInvoiceJpaEntity savedEntity = purchaseInvoiceRepository.save(entity);
        return purchaseInvoiceMapper.toDomain(savedEntity);
    }

    @Override
    public List<PurchaseInvoice> findAll() {
        return purchaseInvoiceRepository.findAll().stream()
                .map(purchaseInvoiceMapper::toDomain)
                .toList();
    }

    @Override
    public List<PurchaseInvoice> findByStatus(PurchaseInvoiceStatus status) {
        return purchaseInvoiceRepository.findByStatus(status).stream()
                .map(purchaseInvoiceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PurchaseInvoice> findById(Long id) {
        return purchaseInvoiceRepository.findById(id)
                .map(purchaseInvoiceMapper::toDomain);
    }

    @Override
    public Optional<PurchaseInvoice> findByInvoiceNoAndVendorCode(String invoiceNo, String vendorCode) {
        return purchaseInvoiceRepository.findByInvoiceNoAndVendorCode(invoiceNo, vendorCode)
                .map(purchaseInvoiceMapper::toDomain);
    }
}
