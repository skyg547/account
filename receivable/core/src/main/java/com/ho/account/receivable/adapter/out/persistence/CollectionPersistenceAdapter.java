package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.CollectionPersistencePort;
import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.mapper.CollectionMapper;
import com.ho.account.receivable.repository.CollectionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CollectionPersistenceAdapter implements CollectionPersistencePort {

    private final CollectionRepository collectionRepository;
    private final CollectionMapper collectionMapper;

    public CollectionPersistenceAdapter(CollectionRepository collectionRepository,
                                         CollectionMapper collectionMapper) {
        this.collectionRepository = collectionRepository;
        this.collectionMapper = collectionMapper;
    }

    @Override
    public Collection save(Collection collection) {
        CollectionJpaEntity entity = collectionMapper.toEntity(collection);
        CollectionJpaEntity savedEntity = collectionRepository.save(entity);
        return collectionMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Collection> findById(Long id) {
        return collectionRepository.findById(id)
                .map(collectionMapper::toDomain);
    }

    @Override
    public List<Collection> findByUnmatched() {
        return collectionRepository.findByStatus(CollectionStatus.UNMATCHED).stream()
                .map(collectionMapper::toDomain)
                .toList();
    }

    @Override
    public List<Collection> findAutoMatchingCandidates() {
        return collectionRepository.findByStatusIn(List.of(
                CollectionStatus.RECEIVED,
                CollectionStatus.UNMATCHED,
                CollectionStatus.PARTIAL_MATCHED)).stream()
                .map(collectionMapper::toDomain)
                .toList();
    }
}
