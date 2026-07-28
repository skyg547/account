package com.ho.account.receivable.application.port.in;

import com.ho.account.receivable.domain.Collection;
import java.util.List;

public interface CollectionUseCase {
    Collection receivePayment(CollectionCommand command);
    void attemptAutoMatching(Long collectionId);
    void manualMatchCollection(ManualMatchingCommand command);
    List<Collection> getUnmatchedCollections();
}