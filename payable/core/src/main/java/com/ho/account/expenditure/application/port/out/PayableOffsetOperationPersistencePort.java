package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.PayableOffsetOperation;
import java.util.Optional;

public interface PayableOffsetOperationPersistencePort {
    Optional<PayableOffsetOperation> findByOperationId(String operationId);
    void save(PayableOffsetOperation operation);
}
