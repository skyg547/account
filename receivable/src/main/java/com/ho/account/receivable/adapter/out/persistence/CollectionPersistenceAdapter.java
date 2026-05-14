package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.CollectionPersistencePort;
import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.repository.CollectionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CollectionPersistenceAdapter implements CollectionPersistencePort {

    private final CollectionRepository collectionRepository;

    public CollectionPersistenceAdapter(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    @Override
    public Collection save(Collection collection) {
        return collectionRepository.save(collection);
    }

    @Override
    public Optional<Collection> findById(Long id) {
        return collectionRepository.findById(id);
    }

    @Override
    public List<Collection> findByUnmatched() {
        return collectionRepository.findByStatus(CollectionStatus.UNMATCHED);
    }
}
