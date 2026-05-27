package com.ho.account.ecl.core.domain.allowance;

import java.time.LocalDate;

public record AllowanceExposureSyncResult(
        LocalDate baseDate,
        int sourceSnapshotCount,
        int customerUpsertCount,
        int accountUpsertCount) {
}
