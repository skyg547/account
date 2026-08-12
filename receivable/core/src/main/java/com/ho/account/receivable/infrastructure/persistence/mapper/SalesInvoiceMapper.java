package com.ho.account.receivable.infrastructure.persistence.mapper;

import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.infrastructure.persistence.entity.SalesInvoiceJpaEntity;
import org.springframework.stereotype.Component;

/**
 * SalesInvoice Domain POJO <-> SalesInvoiceJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * 1. 도메인 계층 독립성 보장:
 *    SalesInvoice 도메인 객체와 JPA 영속성 엔티티(SalesInvoiceJpaEntity) 간 양방향 매핑을 담당합니다.
 *    이를 통해 도메인 모델이 데이터베이스 프레임워크 기술 어노테이션에 종속되지 않습니다.
 * 
 * 2. 영속성 계층 분리:
 *    DB 테이블 구조 변경 시 Mapper만 수정함으로써 도메인 비즈니스 로직에 미치는 영향을 최소화합니다.
 */
@Component
public class SalesInvoiceMapper {

    public SalesInvoice toDomain(SalesInvoiceJpaEntity entity) {
        if (entity == null) return null;
        SalesInvoice domain = SalesInvoice.create(
                entity.getInvoiceNo(),
                entity.getCustomerCode(),
                entity.getIssueDate(),
                entity.getDueDate(),
                entity.getNetAmount(),
                entity.getTaxAmount(),
                entity.getCreatedBy(),
                entity.getDescription()
        );
        domain.setId(entity.getId());
        if (entity.getStatus() != null) {
            if (entity.getStatus() == com.ho.account.receivable.domain.SalesInvoiceStatus.PAID) {
                domain.markAsPaid();
            } else if (entity.getStatus() == com.ho.account.receivable.domain.SalesInvoiceStatus.OVERDUE) {
                domain.markAsOverdue();
            } else {
                domain.setStatus(entity.getStatus());
            }
        }
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public SalesInvoiceJpaEntity toEntity(SalesInvoice domain) {
        if (domain == null) return null;
        SalesInvoiceJpaEntity entity = new SalesInvoiceJpaEntity();
        entity.setId(domain.getId());
        entity.setInvoiceNo(domain.getInvoiceNo());
        entity.setCustomerCode(domain.getCustomerCode());
        entity.setIssueDate(domain.getIssueDate());
        entity.setDueDate(domain.getDueDate());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setTaxAmount(domain.getTaxAmount());
        entity.setNetAmount(domain.getNetAmount());
        entity.setStatus(domain.getStatus());
        entity.setDescription(domain.getDescription());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
