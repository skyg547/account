package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.port.out.AllowanceExposureSyncPort;
import com.ho.account.ecl.core.domain.allowance.AllowanceExposureSyncResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllowanceExposureSyncServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 30);

    @Test
    void syncFromAllowanceSnapshot_upsertsCustomersAndAccounts() {
        FakeAllowanceExposureSyncPort port = new FakeAllowanceExposureSyncPort(5, 3, 5);
        AllowanceExposureSyncService service = new AllowanceExposureSyncService(port);

        AllowanceExposureSyncResult result = service.syncFromAllowanceSnapshot(BASE_DATE);

        assertThat(result.sourceSnapshotCount()).isEqualTo(5);
        assertThat(result.customerUpsertCount()).isEqualTo(3);
        assertThat(result.accountUpsertCount()).isEqualTo(5);
        assertThat(port.customerSynced).isTrue();
        assertThat(port.accountSynced).isTrue();
    }

    @Test
    void syncFromAllowanceSnapshot_skipsWhenNoSnapshotsExist() {
        FakeAllowanceExposureSyncPort port = new FakeAllowanceExposureSyncPort(0, 0, 0);
        AllowanceExposureSyncService service = new AllowanceExposureSyncService(port);

        AllowanceExposureSyncResult result = service.syncFromAllowanceSnapshot(BASE_DATE);

        assertThat(result.sourceSnapshotCount()).isZero();
        assertThat(result.customerUpsertCount()).isZero();
        assertThat(result.accountUpsertCount()).isZero();
        assertThat(port.customerSynced).isFalse();
        assertThat(port.accountSynced).isFalse();
    }

    @Test
    void syncFromAllowanceSnapshot_failsWhenAccountCountDoesNotMatchSnapshotCount() {
        FakeAllowanceExposureSyncPort port = new FakeAllowanceExposureSyncPort(5, 3, 4);
        AllowanceExposureSyncService service = new AllowanceExposureSyncService(port);

        assertThatThrownBy(() -> service.syncFromAllowanceSnapshot(BASE_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("row count mismatch");

        assertThat(port.customerSynced).isTrue();
        assertThat(port.accountSynced).isTrue();
    }

    private static class FakeAllowanceExposureSyncPort implements AllowanceExposureSyncPort {
        private final int sourceCount;
        private final int customerCount;
        private final int accountCount;
        private boolean customerSynced;
        private boolean accountSynced;

        private FakeAllowanceExposureSyncPort(int sourceCount, int customerCount, int accountCount) {
            this.sourceCount = sourceCount;
            this.customerCount = customerCount;
            this.accountCount = accountCount;
        }

        @Override
        public int countSourceSnapshots(LocalDate baseDate) {
            return sourceCount;
        }

        @Override
        public int upsertCustomersFromSnapshot(LocalDate baseDate) {
            customerSynced = true;
            return customerCount;
        }

        @Override
        public int upsertAccountsFromSnapshot(LocalDate baseDate) {
            accountSynced = true;
            return accountCount;
        }
    }
}
