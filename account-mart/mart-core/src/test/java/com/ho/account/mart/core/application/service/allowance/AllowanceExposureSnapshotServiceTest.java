package com.ho.account.mart.core.application.service.allowance;

import com.ho.account.mart.core.application.port.out.AllowanceExposureSnapshotBuildPort;
import com.ho.account.mart.core.domain.allowance.AllowanceExposureSnapshotBuildResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllowanceExposureSnapshotServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 30);

    @Test
    void rebuildSnapshot_rebuildsAfterDeletingCurrentBaseDate() {
        FakeAllowanceExposureSnapshotBuildPort port = new FakeAllowanceExposureSnapshotBuildPort(5, 5);
        AllowanceExposureSnapshotService service = new AllowanceExposureSnapshotService(port);

        AllowanceExposureSnapshotBuildResult result = service.rebuildSnapshot(BASE_DATE);

        assertThat(result.sourcePositionCount()).isEqualTo(5);
        assertThat(result.snapshotRowCount()).isEqualTo(5);
        assertThat(port.deleted).isTrue();
        assertThat(port.inserted).isTrue();
    }

    @Test
    void rebuildSnapshot_clearsSnapshotWhenNoSourcePositionsExist() {
        FakeAllowanceExposureSnapshotBuildPort port = new FakeAllowanceExposureSnapshotBuildPort(0, 0);
        AllowanceExposureSnapshotService service = new AllowanceExposureSnapshotService(port);

        AllowanceExposureSnapshotBuildResult result = service.rebuildSnapshot(BASE_DATE);

        assertThat(result.sourcePositionCount()).isZero();
        assertThat(result.snapshotRowCount()).isZero();
        assertThat(port.deleted).isTrue();
        assertThat(port.inserted).isFalse();
    }

    @Test
    void rebuildSnapshot_failsWhenSnapshotRowCountDoesNotMatchSourcePositions() {
        FakeAllowanceExposureSnapshotBuildPort port = new FakeAllowanceExposureSnapshotBuildPort(5, 4);
        AllowanceExposureSnapshotService service = new AllowanceExposureSnapshotService(port);

        assertThatThrownBy(() -> service.rebuildSnapshot(BASE_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("row count mismatch");

        assertThat(port.deleted).isTrue();
        assertThat(port.inserted).isTrue();
    }

    private static class FakeAllowanceExposureSnapshotBuildPort implements AllowanceExposureSnapshotBuildPort {
        private final int sourcePositionCount;
        private final int insertedRowCount;
        private boolean deleted;
        private boolean inserted;

        private FakeAllowanceExposureSnapshotBuildPort(int sourcePositionCount, int insertedRowCount) {
            this.sourcePositionCount = sourcePositionCount;
            this.insertedRowCount = insertedRowCount;
        }

        @Override
        public int countSourcePositions(LocalDate baseDate) {
            return sourcePositionCount;
        }

        @Override
        public void deleteByBaseDate(LocalDate baseDate) {
            deleted = true;
        }

        @Override
        public int insertFromIntegratedPositions(LocalDate baseDate) {
            inserted = true;
            return insertedRowCount;
        }
    }
}
