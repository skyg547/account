package com.ho.account.receivable.infrastructure.persistence.mapper;

import com.ho.account.receivable.domain.UnmatchedCollection;
import com.ho.account.receivable.infrastructure.persistence.entity.UnmatchedCollectionJpaEntity;
import org.springframework.stereotype.Component;

/**
 * UnmatchedCollection Domain POJO <-> UnmatchedCollectionJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * UnmatchedCollection 도메인 객체와 JPA 영속성 엔티티(UnmatchedCollectionJpaEntity) 간 양방향 데이터 매핑을 전담합니다.
 */
@Component
public class UnmatchedCollectionMapper {

    private final CollectionMapper collectionMapper;

    public UnmatchedCollectionMapper(CollectionMapper collectionMapper) {
        this.collectionMapper = collectionMapper;
    }

    public UnmatchedCollection toDomain(UnmatchedCollectionJpaEntity entity) {
        if (entity == null) return null;
        UnmatchedCollection domain = new UnmatchedCollection();
        domain.setId(entity.getId());
        domain.setCollection(collectionMapper.toDomain(entity.getCollection()));
        domain.setReason(entity.getReason());
        domain.setStatus(entity.getStatus());
        domain.setResolvedBy(entity.getResolvedBy());
        domain.setResolvedAt(entity.getResolvedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public UnmatchedCollectionJpaEntity toEntity(UnmatchedCollection domain) {
        if (domain == null) return null;
        UnmatchedCollectionJpaEntity entity = new UnmatchedCollectionJpaEntity();
        entity.setId(domain.getId());
        entity.setCollection(collectionMapper.toEntity(domain.getCollection()));
        entity.setReason(domain.getReason());
        entity.setStatus(domain.getStatus());
        entity.setResolvedBy(domain.getResolvedBy());
        entity.setResolvedAt(domain.getResolvedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
