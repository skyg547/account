package com.risk.mart.core.application.service.allowance;

import com.risk.mart.core.application.port.out.AllowanceExposureSnapshotBuildPort;
import com.risk.mart.core.domain.allowance.AllowanceExposureSnapshotBuildResult;
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
public class AllowanceExposureSnapshotService {

    private final AllowanceExposureSnapshotBuildPort snapshotBuildPort;

    @Transactional
    public AllowanceExposureSnapshotBuildResult rebuildSnapshot(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "baseDate must not be null");

        int sourcePositionCount = snapshotBuildPort.countSourcePositions(baseDate);
        snapshotBuildPort.deleteByBaseDate(baseDate);

        if (sourcePositionCount == 0) {
            log.info("[Allowance Exposure Snapshot] no CDM positions. baseDate={}", baseDate);
            return new AllowanceExposureSnapshotBuildResult(baseDate, 0, 0);
        }

        int snapshotRowCount = snapshotBuildPort.insertFromIntegratedPositions(baseDate);
        if (snapshotRowCount != sourcePositionCount) {
            throw new IllegalStateException("Allowance exposure snapshot row count mismatch. baseDate="
                    + baseDate + ", sourcePositionCount=" + sourcePositionCount
                    + ", snapshotRowCount=" + snapshotRowCount);
        }

        log.info("[Allowance Exposure Snapshot] rebuilt. baseDate={}, sourcePositions={}, snapshotRows={}",
                baseDate, sourcePositionCount, snapshotRowCount);
        return new AllowanceExposureSnapshotBuildResult(baseDate, sourcePositionCount, snapshotRowCount);
    }
}
