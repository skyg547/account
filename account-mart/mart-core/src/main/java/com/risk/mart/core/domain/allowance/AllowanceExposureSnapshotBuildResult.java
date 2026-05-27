package com.risk.mart.core.domain.allowance;

import java.time.LocalDate;

public record AllowanceExposureSnapshotBuildResult(
        LocalDate baseDate,
        int sourcePositionCount,
        int snapshotRowCount) {
}
