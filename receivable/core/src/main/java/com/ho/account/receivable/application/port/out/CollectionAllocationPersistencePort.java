package com.ho.account.receivable.application.port.out;

import com.ho.account.receivable.domain.CollectionAllocation;
import java.util.Optional;

public interface CollectionAllocationPersistencePort {

    CollectionAllocation save(CollectionAllocation allocation);
    Optional<CollectionAllocation> findByCollectionIdAndOperationId(Long collectionId, String operationId);
}
