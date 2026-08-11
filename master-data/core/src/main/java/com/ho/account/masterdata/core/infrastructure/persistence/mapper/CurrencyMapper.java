package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.CurrencyEntity;
import org.springframework.stereotype.Component;

/**
 * Currency Domain POJO <-> CurrencyEntity 양방향 변환 Mapper.
 * 
 * 🐣 [Data Mapper 패턴 교육적 주석]
 * Currency 도메인 객체와 JPA 영속성 엔티티(CurrencyEntity) 간 양방향 데이터 매핑을 전담합니다.
 */
@Component
public class CurrencyMapper {

    public Currency toDomain(CurrencyEntity entity) {
        if (entity == null) return null;
        Currency domain = new Currency();
        domain.setId(entity.getId());
        domain.setCurrencyCode(entity.getCurrencyCode());
        domain.setCurrencyName(entity.getCurrencyName());
        domain.setSymbol(entity.getSymbol());
        domain.setValidFrom(entity.getValidFrom());
        domain.setValidTo(entity.getValidTo());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        return domain;
    }

    public CurrencyEntity toEntity(Currency domain) {
        if (domain == null) return null;
        CurrencyEntity entity = new CurrencyEntity();
        entity.setId(domain.getId());
        entity.setCurrencyCode(domain.getCurrencyCode());
        entity.setCurrencyName(domain.getCurrencyName());
        entity.setSymbol(domain.getSymbol());
        entity.setValidFrom(domain.getValidFrom());
        entity.setValidTo(domain.getValidTo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }
}
