package com.ho.account.receivable.infrastructure.persistence.mapper;

import com.ho.account.receivable.domain.CollectionAllocation;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionAllocationJpaEntity;
import org.springframework.stereotype.Component;

/**
 * CollectionAllocation Domain POJO <-> CollectionAllocationJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * CollectionAllocation 도메인 객체와 JPA 영속성 엔티티(CollectionAllocationJpaEntity) 간 매핑을 전담합니다.
 */
@Component
public class CollectionAllocationMapper {

    private final CollectionMapper collectionMapper;
    private final ReceivableMapper receivableMapper;

    public CollectionAllocationMapper(CollectionMapper collectionMapper,
                                       ReceivableMapper receivableMapper) {
        this.collectionMapper = collectionMapper;
        this.receivableMapper = receivableMapper;
    }

    public CollectionAllocation toDomain(CollectionAllocationJpaEntity entity) {
        if (entity == null) return null;
        CollectionAllocation domain = CollectionAllocation.record(
                collectionMapper.toDomain(entity.getCollection()),
                receivableMapper.toDomain(entity.getReceivable()),
                entity.getMatchedAmount()
        );
        domain.setId(entity.getId());
        domain.setResidualCollectionAmount(entity.getResidualCollectionAmount());
        domain.setResidualReceivableAmount(entity.getResidualReceivableAmount());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public CollectionAllocationJpaEntity toEntity(CollectionAllocation domain) {
        if (domain == null) return null;
        CollectionAllocationJpaEntity entity = new CollectionAllocationJpaEntity();
        entity.setId(domain.getId());
        entity.setCollection(collectionMapper.toEntity(domain.getCollection()));
        entity.setReceivable(receivableMapper.toEntity(domain.getReceivable()));
        entity.setMatchedAmount(domain.getMatchedAmount());
        entity.setResidualCollectionAmount(domain.getResidualCollectionAmount());
        entity.setResidualReceivableAmount(domain.getResidualReceivableAmount());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
