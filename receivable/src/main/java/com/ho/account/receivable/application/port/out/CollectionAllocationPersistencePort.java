package com.ho.account.receivable.application.port.out;

import com.ho.account.receivable.domain.CollectionAllocation;

public interface CollectionAllocationPersistencePort {

    CollectionAllocation save(CollectionAllocation allocation);
}
