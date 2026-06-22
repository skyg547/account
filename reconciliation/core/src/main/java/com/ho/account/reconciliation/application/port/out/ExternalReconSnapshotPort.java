package com.ho.account.reconciliation.application.port.out;

public interface ExternalReconSnapshotPort {

    ExternalReconSnapshot loadSnapshot(ExternalReconSnapshotRequest request);
}
