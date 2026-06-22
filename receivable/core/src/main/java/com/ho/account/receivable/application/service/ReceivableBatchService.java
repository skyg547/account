package com.ho.account.receivable.application.service;

import com.ho.account.receivable.application.port.in.CollectionUseCase;
import com.ho.account.receivable.application.port.in.ReceivableBatchUseCase;
import com.ho.account.receivable.application.port.out.CollectionPersistencePort;
import com.ho.account.receivable.domain.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReceivableBatchService implements ReceivableBatchUseCase {

    private final CollectionPersistencePort collectionPersistencePort;
    private final CollectionUseCase collectionUseCase;

    public ReceivableBatchService(
            CollectionPersistencePort collectionPersistencePort,
            CollectionUseCase collectionUseCase) {
        this.collectionPersistencePort = collectionPersistencePort;
        this.collectionUseCase = collectionUseCase;
    }

    @Override
    public AutoMatchingBatchResult runAutoMatching() {
        List<Collection> candidates = collectionPersistencePort.findAutoMatchingCandidates();
        int attempted = 0;
        for (Collection collection : candidates) {
            if (collection.getId() == null) {
                continue;
            }
            collectionUseCase.attemptAutoMatching(collection.getId());
            attempted++;
        }
        return new AutoMatchingBatchResult(candidates.size(), attempted);
    }
}
