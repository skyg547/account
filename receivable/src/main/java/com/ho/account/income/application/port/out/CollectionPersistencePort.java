package com.ho.account.income.application.port.out;

import com.ho.account.income.domain.Collection;
import java.util.List;
import java.util.Optional;

public interface CollectionPersistencePort {
    Collection save(Collection collection);
    Optional<Collection> findById(Long id);
    List<Collection> findByUnmatched();
}
