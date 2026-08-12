package com.ho.account.expenditure.infrastructure.persistence.mapper;

import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.infrastructure.persistence.entity.PurchaseInvoiceJpaEntity;
import org.springframework.stereotype.Component;

/**
 * PurchaseInvoice Domain POJO <-> PurchaseInvoiceJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * 
 * 1. 도메인 계층 독립성 보장:
 *    Data Mapper 패턴은 Domain 객체(PurchaseInvoice)와 DB JPA Entity(PurchaseInvoiceJpaEntity) 간의 매핑을 
 *    Infrastructure 계층에서 전담함으로써 도메인 모델이 Persistence 기술(JPA, ORM 어노테이션 등)에 종속되는 것을 완전히 차단합니다.
 * 
 * 2. 객체-관계 불일치 해소 (Object-Relational Impedance Mismatch):
 *    도메인 모델의 캡슐화 및 비즈니스 행동과 DB 테이블 구조 변경이 서로에게 파급 효과를 미치지 않도록 분리합니다.
 */
@Component
public class PurchaseInvoiceMapper {

    public PurchaseInvoice toDomain(PurchaseInvoiceJpaEntity entity) {
        if (entity == null) return null;
        PurchaseInvoice domain = new PurchaseInvoice();
        domain.setId(entity.getId());
        domain.setInvoiceNo(entity.getInvoiceNo());
        domain.setVendorCode(entity.getVendorCode());
        domain.setIssueDate(entity.getIssueDate());
        domain.setDueDate(entity.getDueDate());
        domain.setTotalAmount(entity.getTotalAmount());
        domain.setTaxAmount(entity.getTaxAmount());
        domain.setNetAmount(entity.getNetAmount());
        domain.setStatus(entity.getStatus());
        domain.setJournalEntryId(entity.getJournalEntryId());
        domain.setDescription(entity.getDescription());
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public PurchaseInvoiceJpaEntity toEntity(PurchaseInvoice domain) {
        if (domain == null) return null;
        PurchaseInvoiceJpaEntity entity = new PurchaseInvoiceJpaEntity();
        entity.setId(domain.getId());
        entity.setInvoiceNo(domain.getInvoiceNo());
        entity.setVendorCode(domain.getVendorCode());
        entity.setIssueDate(domain.getIssueDate());
        entity.setDueDate(domain.getDueDate());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setTaxAmount(domain.getTaxAmount());
        entity.setNetAmount(domain.getNetAmount());
        entity.setStatus(domain.getStatus());
        entity.setJournalEntryId(domain.getJournalEntryId());
        entity.setDescription(domain.getDescription());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
