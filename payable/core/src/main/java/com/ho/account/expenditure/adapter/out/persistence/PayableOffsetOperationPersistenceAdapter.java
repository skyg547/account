package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PayableOffsetOperationPersistencePort;
import com.ho.account.expenditure.domain.PayableOffsetOperation;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableOffsetOperationJpaEntity;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableOffsetOperationId;
import com.ho.account.expenditure.repository.PayableOffsetOperationRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PayableOffsetOperationPersistenceAdapter implements PayableOffsetOperationPersistencePort {
    private final PayableOffsetOperationRepository repository;

    public PayableOffsetOperationPersistenceAdapter(PayableOffsetOperationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PayableOffsetOperation> findByPayableIdAndOperationId(Long payableId, String operationId) {
        return repository.findById(new PayableOffsetOperationId(payableId, operationId)).map(entity -> new PayableOffsetOperation(
                entity.getOperationId(), entity.getPayableId(), entity.getAdvancePaymentId(),
                entity.getAmount(), entity.getAccountingDate(), entity.getJournalEntryId()));
    }

    @Override
    public void save(PayableOffsetOperation operation) {
        PayableOffsetOperationJpaEntity entity = new PayableOffsetOperationJpaEntity();
        entity.setOperationId(operation.operationId());
        entity.setPayableId(operation.payableId());
        entity.setAdvancePaymentId(operation.advancePaymentId());
        entity.setAmount(operation.amount());
        entity.setAccountingDate(operation.accountingDate());
        entity.setJournalEntryId(operation.journalEntryId());
        repository.save(entity);
    }
}
