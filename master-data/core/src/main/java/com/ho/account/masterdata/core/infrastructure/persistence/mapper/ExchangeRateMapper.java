package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import com.ho.account.masterdata.core.domain.model.ExchangeRate;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.ExchangeRateEntity;
import org.springframework.stereotype.Component;

/**
 * ExchangeRate Domain POJO <-> ExchangeRateEntity 양방향 변환 Mapper.
 * 
 * 🐣 [Data Mapper 패턴 교육적 주석]
 * ExchangeRate 도메인 객체와 JPA 영속성 엔티티(ExchangeRateEntity) 간 변환 매퍼입니다.
 */
@Component
public class ExchangeRateMapper {

    public ExchangeRate toDomain(ExchangeRateEntity entity) {
        if (entity == null) return null;
        ExchangeRate domain = new ExchangeRate();
        domain.setId(entity.getId());
        domain.setFromCurrencyCode(entity.getFromCurrencyCode());
        domain.setToCurrencyCode(entity.getToCurrencyCode());
        domain.setRate(entity.getRate());
        domain.setEffectiveDate(entity.getEffectiveDate());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public ExchangeRateEntity toEntity(ExchangeRate domain) {
        if (domain == null) return null;
        ExchangeRateEntity entity = new ExchangeRateEntity();
        entity.setId(domain.getId());
        entity.setFromCurrencyCode(domain.getFromCurrencyCode());
        entity.setToCurrencyCode(domain.getToCurrencyCode());
        entity.setRate(domain.getRate());
        entity.setEffectiveDate(domain.getEffectiveDate());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
