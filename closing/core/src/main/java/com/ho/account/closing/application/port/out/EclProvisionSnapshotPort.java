package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.EclProvisionSnapshot;
import java.util.List;
import java.util.Map;

/** Binds operation keys to immutable ECL source snapshots before any Journal write. */
public interface EclProvisionSnapshotPort {
    void recordAll(List<EclProvisionSnapshot> snapshots, Map<String, String> postedNoopReferences);
}
