package com.risk.mart.core.infrastructure.persistence.entity.allowance;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
public class AllowanceExposureSnapshotId implements Serializable {

    private LocalDate baseDate;
    private String exposureId;
}
