package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.CollectionAllocationPersistencePort;
import com.ho.account.receivable.domain.CollectionAllocation;
import com.ho.account.receivable.repository.CollectionAllocationRepository;
import org.springframework.stereotype.Component;

@Component
public class CollectionAllocationPersistenceAdapter implements CollectionAllocationPersistencePort {

    private final CollectionAllocationRepository repository;

    public CollectionAllocationPersistenceAdapter(CollectionAllocationRepository repository) {
        this.repository = repository;
    }

    @Override
    public CollectionAllocation save(CollectionAllocation allocation) {
        return repository.save(allocation);
    }
}
