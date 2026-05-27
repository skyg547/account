package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.port.out.AllowanceExposureSyncPort;
import com.ho.account.ecl.core.domain.allowance.AllowanceExposureSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllowanceExposureSyncService {

    private final AllowanceExposureSyncPort syncPort;

    @Transactional
    public AllowanceExposureSyncResult syncFromAllowanceSnapshot(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "baseDate must not be null");

        int sourceSnapshotCount = syncPort.countSourceSnapshots(baseDate);
        if (sourceSnapshotCount == 0) {
            log.warn("[Allowance Exposure Sync] no source snapshots. baseDate={}", baseDate);
            return new AllowanceExposureSyncResult(baseDate, 0, 0, 0);
        }

        int customerUpsertCount = syncPort.upsertCustomersFromSnapshot(baseDate);
        int accountUpsertCount = syncPort.upsertAccountsFromSnapshot(baseDate);
        if (accountUpsertCount != sourceSnapshotCount) {
            throw new IllegalStateException("Allowance exposure account sync row count mismatch. baseDate="
                    + baseDate + ", sourceSnapshotCount=" + sourceSnapshotCount
                    + ", accountUpsertCount=" + accountUpsertCount);
        }

        log.info("[Allowance Exposure Sync] completed. baseDate={}, sourceSnapshots={}, customers={}, accounts={}",
                baseDate, sourceSnapshotCount, customerUpsertCount, accountUpsertCount);
        return new AllowanceExposureSyncResult(
                baseDate,
                sourceSnapshotCount,
                customerUpsertCount,
                accountUpsertCount);
    }
}
