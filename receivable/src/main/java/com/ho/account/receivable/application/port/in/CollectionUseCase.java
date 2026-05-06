package com.ho.account.receivable.application.port.in;

import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.Receivable;
import java.math.BigDecimal;
import java.util.List;

public interface CollectionUseCase {
    Collection receivePayment(Collection collection);
    void attemptAutoMatching(Long collectionId);
    void manualMatchCollection(Long collectionId, Long receivableId, BigDecimal amount);
    List<Collection> getUnmatchedCollections();
}
