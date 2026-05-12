package com.ho.account.reconciliation.infrastructure.adapter;

import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshot;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest;
import com.ho.account.reconciliation.infrastructure.persistence.ExternalReconStageRecordRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExternalReconStageSnapshotAdapter implements ExternalReconSnapshotPort {

    private final ExternalReconStageRecordRepository repository;

    public ExternalReconStageSnapshotAdapter(ExternalReconStageRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExternalReconSnapshot loadSnapshot(ExternalReconSnapshotRequest request) {
        ExternalReconStageRecordRepository.SnapshotAggregateProjection projection = repository.summarize(
                request.unitId(),
                request.stageCode(),
                request.reconciliationDate(),
                request.productCode(),
                request.currencyCode(),
                request.legalEntityCode()
        );
        if (projection == null) {
            return ExternalReconSnapshot.zero();
        }

        long count = projection.getItemCount() == null ? 0L : projection.getItemCount();
        BigDecimal amount = projection.getTotalAmount() == null ? BigDecimal.ZERO : projection.getTotalAmount();
        return new ExternalReconSnapshot(count, amount);
    }
}
