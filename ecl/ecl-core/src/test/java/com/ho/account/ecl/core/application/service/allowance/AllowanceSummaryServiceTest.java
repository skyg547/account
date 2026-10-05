package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.port.out.AllowanceSummaryBuildPort;
import com.ho.account.ecl.core.domain.allowance.AllowanceSummaryBuildResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllowanceSummaryServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 30);

    @Test
    void rebuildAllowanceSummary_rebuildsAfterMappingValidation() {
        FakeAllowanceSummaryBuildPort port = new FakeAllowanceSummaryBuildPort(10, 0, 3);
        AllowanceSummaryService service = new AllowanceSummaryService(port);

        AllowanceSummaryBuildResult result = service.rebuildAllowanceSummary(BASE_DATE, "RUN-1", "v1");

        assertThat(result.sourceResultCount()).isEqualTo(10);
        assertThat(result.summaryRowCount()).isEqualTo(3);
        assertThat(port.deleted).isTrue();
        assertThat(port.inserted).isTrue();
    }

    @Test
    void rebuildAllowanceSummary_keepsPreviousSummaryWhenMappingIsMissing() {
        FakeAllowanceSummaryBuildPort port = new FakeAllowanceSummaryBuildPort(10, 2, 0);
        AllowanceSummaryService service = new AllowanceSummaryService(port);

        assertThatThrownBy(() -> service.rebuildAllowanceSummary(BASE_DATE, "RUN-1", "v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing allowance account mappings");

        assertThat(port.deleted).isFalse();
        assertThat(port.inserted).isFalse();
    }

    @Test
    void rebuildAllowanceSummary_keepsPreviousSummaryWhenNoCompletedResultsExist() {
        FakeAllowanceSummaryBuildPort port = new FakeAllowanceSummaryBuildPort(0, 0, 0);
        AllowanceSummaryService service = new AllowanceSummaryService(port);

        assertThatThrownBy(() -> service.rebuildAllowanceSummary(BASE_DATE, "RUN-1", "v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed ECL results")
                .hasMessageContaining(BASE_DATE.toString());

        assertThat(port.deleted).isFalse();
        assertThat(port.inserted).isFalse();
    }

    private static class FakeAllowanceSummaryBuildPort implements AllowanceSummaryBuildPort {
        private final int eligibleCount;
        private final int missingMappingCount;
        private final int insertedCount;
        private boolean deleted;
        private boolean inserted;

        private FakeAllowanceSummaryBuildPort(int eligibleCount, int missingMappingCount, int insertedCount) {
            this.eligibleCount = eligibleCount;
            this.missingMappingCount = missingMappingCount;
            this.insertedCount = insertedCount;
        }

        @Override
        public int countEligibleResults(LocalDate baseDate) {
            return eligibleCount;
        }

        @Override
        public int countMissingAccountMappings(LocalDate baseDate) {
            return missingMappingCount;
        }

        @Override
        public void deleteByBaseDate(LocalDate baseDate) {
            deleted = true;
        }

        @Override
        public int insertSummariesFromAllowanceResults(LocalDate baseDate, String runId, String modelVersion) {
            inserted = true;
            return insertedCount;
        }
    }
}
