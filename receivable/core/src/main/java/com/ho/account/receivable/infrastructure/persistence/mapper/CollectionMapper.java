package com.ho.account.receivable.infrastructure.persistence.mapper;

import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Collection Domain POJO <-> CollectionJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * Collection 도메인 객체와 JPA 영속성 엔티티(CollectionJpaEntity) 간 양방향 데이터 매핑을 담당합니다.
 */
@Component
public class CollectionMapper {

    public Collection toDomain(CollectionJpaEntity entity) {
        if (entity == null) return null;
        Collection domain = new Collection();
        domain.setId(entity.getId());
        domain.setCollectionDate(entity.getCollectionDate());
        domain.setCustomerCode(entity.getCustomerCode());
        domain.setAmount(entity.getAmount());
        domain.setMatchedAmount(entity.getMatchedAmount());
        domain.setBankAccount(entity.getBankAccount());
        domain.setVirtualAccount(entity.getVirtualAccount());
        domain.setReferenceNo(entity.getReferenceNo());
        domain.setStatus(entity.getStatus());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public CollectionJpaEntity toEntity(Collection domain) {
        if (domain == null) return null;
        CollectionJpaEntity entity = new CollectionJpaEntity();
        entity.setId(domain.getId());
        entity.setCollectionDate(domain.getCollectionDate());
        entity.setCustomerCode(domain.getCustomerCode());
        entity.setAmount(domain.getAmount());
        entity.setMatchedAmount(domain.getMatchedAmount());
        entity.setBankAccount(domain.getBankAccount());
        entity.setVirtualAccount(domain.getVirtualAccount());
        entity.setReferenceNo(domain.getReferenceNo());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
