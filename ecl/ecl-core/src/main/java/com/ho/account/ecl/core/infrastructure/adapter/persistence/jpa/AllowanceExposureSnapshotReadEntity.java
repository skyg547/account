package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Minimal read model for checking whether an account belongs to a dated Mart snapshot. */
@Entity(name = "EclAllowanceExposureSnapshotRead")
@Table(name = "allowance_exposure_snapshots")
@IdClass(AllowanceExposureSnapshotReadId.class)
public class AllowanceExposureSnapshotReadEntity {

    @Id
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Id
    @Column(name = "exposure_id", nullable = false, length = 80)
    private String exposureId;

    @Column(name = "source_account_no", nullable = false, length = 50)
    private String sourceAccountNo;
}
