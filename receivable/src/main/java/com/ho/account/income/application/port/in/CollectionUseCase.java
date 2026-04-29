package com.ho.account.income.application.port.in;

import com.ho.account.income.domain.Collection;
import com.ho.account.income.domain.Receivable;
import java.math.BigDecimal;
import java.util.List;

public interface CollectionUseCase {
    Collection receivePayment(Collection collection);
    void attemptAutoMatching(Long collectionId);
    void manualMatchCollection(Long collectionId, Long receivableId, BigDecimal amount);
    List<Collection> getUnmatchedCollections();
}
