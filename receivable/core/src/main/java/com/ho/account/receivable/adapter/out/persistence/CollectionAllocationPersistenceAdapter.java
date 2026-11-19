package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.CollectionAllocationPersistencePort;
import com.ho.account.receivable.domain.CollectionAllocation;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionAllocationJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.mapper.CollectionAllocationMapper;
import com.ho.account.receivable.repository.CollectionAllocationRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

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
        // Reserve the unique operation before calling journal-ledger so duplicates fail in this transaction.
        CollectionAllocationJpaEntity savedEntity = repository.saveAndFlush(entity);
        return collectionAllocationMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<CollectionAllocation> findByCollectionIdAndOperationId(Long collectionId, String operationId) {
        return repository.findByCollectionIdAndOperationId(collectionId, operationId)
                .map(collectionAllocationMapper::toDomain);
    }
}
