package com.ho.account.receivable.infrastructure.persistence.mapper;

import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.infrastructure.persistence.entity.ReceivableJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Receivable Domain POJO <-> ReceivableJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * Receivable 도메인 객체와 JPA 영속성 엔티티(ReceivableJpaEntity) 간 양방향 데이터 매핑을 전담합니다.
 * SalesInvoice 연관 객체 변환 시 SalesInvoiceMapper를 연계하여 도메인 모델 그래프 매핑을 일관되게 관리합니다.
 */
@Component
public class ReceivableMapper {

    private final SalesInvoiceMapper salesInvoiceMapper;

    public ReceivableMapper(SalesInvoiceMapper salesInvoiceMapper) {
        this.salesInvoiceMapper = salesInvoiceMapper;
    }

    public Receivable toDomain(ReceivableJpaEntity entity) {
        if (entity == null) return null;
        Receivable domain = new Receivable();
        domain.setId(entity.getId());
        domain.setSalesInvoice(salesInvoiceMapper.toDomain(entity.getSalesInvoice()));
        domain.setCustomerCode(entity.getCustomerCode());
        domain.setOriginalAmount(entity.getOriginalAmount());
        domain.setOutstandingAmount(entity.getOutstandingAmount());
        domain.setDueDate(entity.getDueDate());
        domain.setStatus(entity.getStatus());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public ReceivableJpaEntity toEntity(Receivable domain) {
        if (domain == null) return null;
        ReceivableJpaEntity entity = new ReceivableJpaEntity();
        entity.setId(domain.getId());
        entity.setSalesInvoice(salesInvoiceMapper.toEntity(domain.getSalesInvoice()));
        entity.setCustomerCode(domain.getCustomerCode());
        entity.setOriginalAmount(domain.getOriginalAmount());
        entity.setOutstandingAmount(domain.getOutstandingAmount());
        entity.setDueDate(domain.getDueDate());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
