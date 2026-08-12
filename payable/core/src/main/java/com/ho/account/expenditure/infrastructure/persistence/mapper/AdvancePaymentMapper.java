package com.ho.account.expenditure.infrastructure.persistence.mapper;

import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.infrastructure.persistence.entity.AdvancePaymentJpaEntity;
import org.springframework.stereotype.Component;

/**
 * AdvancePayment Domain POJO <-> AdvancePaymentJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * AdvancePayment 도메인 객체와 JPA 영속성 엔티티(AdvancePaymentJpaEntity) 간 양방향 데이터 매핑을 전담합니다.
 */
@Component
public class AdvancePaymentMapper {

    public AdvancePayment toDomain(AdvancePaymentJpaEntity entity) {
        if (entity == null) return null;
        AdvancePayment domain = new AdvancePayment();
        domain.setId(entity.getId());
        domain.setVendorCode(entity.getVendorCode());
        domain.setPaymentDate(entity.getPaymentDate());
        domain.setAmount(entity.getAmount());
        domain.setOutstandingAmount(entity.getOutstandingAmount());
        domain.setDescription(entity.getDescription());
        domain.setStatus(entity.getStatus());
        domain.setJournalEntryId(entity.getJournalEntryId());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public AdvancePaymentJpaEntity toEntity(AdvancePayment domain) {
        if (domain == null) return null;
        AdvancePaymentJpaEntity entity = new AdvancePaymentJpaEntity();
        entity.setId(domain.getId());
        entity.setVendorCode(domain.getVendorCode());
        entity.setPaymentDate(domain.getPaymentDate());
        entity.setAmount(domain.getAmount());
        entity.setOutstandingAmount(domain.getOutstandingAmount());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus());
        entity.setJournalEntryId(domain.getJournalEntryId());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
