package com.ho.account.reconciliation.application.port.out;

import com.ho.account.reconciliation.domain.ReconciliationItem;
import java.util.List;

public interface ExternalReconSnapshotPort {

    ExternalReconSnapshot loadSnapshot(ExternalReconSnapshotRequest request);

    default List<ReconciliationItem> loadItems(ExternalReconSnapshotRequest request) {
        return List.of();
    }
}
