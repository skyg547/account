package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import com.ho.account.masterdata.core.domain.model.TaxProfile;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.TaxProfileEntity;
import org.springframework.stereotype.Component;

/**
 * TaxProfile Domain POJO <-> TaxProfileEntity 양방향 변환 Mapper.
 * 
 * 🐣 [Data Mapper 패턴 교육적 주석]
 * TaxProfile 도메인 객체와 JPA 영속성 엔티티(TaxProfileEntity) 간 변환 매퍼입니다.
 */
@Component
public class TaxProfileMapper {

    public TaxProfile toDomain(TaxProfileEntity entity) {
        if (entity == null) return null;
        TaxProfile domain = new TaxProfile();
        domain.setId(entity.getId());
        domain.setTaxCode(entity.getTaxCode());
        domain.setName(entity.getName());
        domain.setTaxType(entity.getTaxType());
        domain.setTaxRate(entity.getTaxRate());
        domain.setTaxAccountCode(entity.getTaxAccountCode());
        domain.setValidFrom(entity.getValidFrom());
        domain.setValidTo(entity.getValidTo());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        return domain;
    }

    public TaxProfileEntity toEntity(TaxProfile domain) {
        if (domain == null) return null;
        TaxProfileEntity entity = new TaxProfileEntity();
        entity.setId(domain.getId());
        entity.setTaxCode(domain.getTaxCode());
        entity.setName(domain.getName());
        entity.setTaxType(domain.getTaxType());
        entity.setTaxRate(domain.getTaxRate());
        entity.setTaxAccountCode(domain.getTaxAccountCode());
        entity.setValidFrom(domain.getValidFrom());
        entity.setValidTo(domain.getValidTo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }
}
