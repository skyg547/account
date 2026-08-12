package com.ho.account.expenditure.infrastructure.persistence.mapper;

import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Payable Domain POJO <-> PayableJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * Payable 도메인 객체와 JPA 영속성 엔티티(PayableJpaEntity) 간 양방향 데이터 매핑을 전담합니다.
 */
@Component
public class PayableMapper {

    public Payable toDomain(PayableJpaEntity entity) {
        if (entity == null) return null;
        Payable domain = new Payable();
        domain.setId(entity.getId());
        domain.setPurchaseInvoiceNo(entity.getPurchaseInvoiceNo());
        domain.setPurchaseInvoiceVendorCode(entity.getPurchaseInvoiceVendorCode());
        domain.setVendorCode(entity.getVendorCode());
        domain.setOriginalAmount(entity.getOriginalAmount());
        domain.setOutstandingAmount(entity.getOutstandingAmount());
        domain.setDueDate(entity.getDueDate());
        domain.setStatus(entity.getStatus());
        domain.setJournalEntryId(entity.getJournalEntryId());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public PayableJpaEntity toEntity(Payable domain) {
        if (domain == null) return null;
        PayableJpaEntity entity = new PayableJpaEntity();
        entity.setId(domain.getId());
        entity.setPurchaseInvoiceNo(domain.getPurchaseInvoiceNo());
        entity.setPurchaseInvoiceVendorCode(domain.getPurchaseInvoiceVendorCode());
        entity.setVendorCode(domain.getVendorCode());
        entity.setOriginalAmount(domain.getOriginalAmount());
        entity.setOutstandingAmount(domain.getOutstandingAmount());
        entity.setDueDate(domain.getDueDate());
        entity.setStatus(domain.getStatus());
        entity.setJournalEntryId(domain.getJournalEntryId());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
