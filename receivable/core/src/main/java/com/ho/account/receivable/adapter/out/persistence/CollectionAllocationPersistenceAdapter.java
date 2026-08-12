package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.CollectionAllocationPersistencePort;
import com.ho.account.receivable.domain.CollectionAllocation;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionAllocationJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.mapper.CollectionAllocationMapper;
import com.ho.account.receivable.repository.CollectionAllocationRepository;
import org.springframework.stereotype.Component;

@Component
public class CollectionAllocationPersistenceAdapter implements CollectionAllocationPersistencePort {

    private final CollectionAllocationRepository repository;
    private final CollectionAllocationMapper collectionAllocationMapper;

    public CollectionAllocationPersistenceAdapter(CollectionAllocationRepository repository,
                                                   CollectionAllocationMapper collectionAllocationMapper) {
        this.repository = repository;
        this.collectionAllocationMapper = collectionAllocationMapper;
    }

    @Override
    public CollectionAllocation save(CollectionAllocation allocation) {
        CollectionAllocationJpaEntity entity = collectionAllocationMapper.toEntity(allocation);
        CollectionAllocationJpaEntity savedEntity = repository.save(entity);
        return collectionAllocationMapper.toDomain(savedEntity);
    }
}
