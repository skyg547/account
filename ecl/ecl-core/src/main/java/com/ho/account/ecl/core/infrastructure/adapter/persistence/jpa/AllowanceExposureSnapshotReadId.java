package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** Composite identity of a Mart exposure snapshot row. */
public class AllowanceExposureSnapshotReadId implements Serializable {
    private LocalDate baseDate;
    private String exposureId;

    public AllowanceExposureSnapshotReadId() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AllowanceExposureSnapshotReadId that)) return false;
        return Objects.equals(baseDate, that.baseDate) && Objects.equals(exposureId, that.exposureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseDate, exposureId);
    }
}
