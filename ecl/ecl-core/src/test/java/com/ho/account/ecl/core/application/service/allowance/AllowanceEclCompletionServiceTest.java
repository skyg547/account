package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.port.out.AllowanceEclCompletionPort;
import com.ho.account.ecl.core.domain.allowance.AllowanceEclCompletionResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllowanceEclCompletionServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 30);

    @Test
    void completeCalculatedEclResults_marksCalculatedRowsCompleted() {
        FakeAllowanceEclCompletionPort port = new FakeAllowanceEclCompletionPort(7, 7);
        AllowanceEclCompletionService service = new AllowanceEclCompletionService(port);

        AllowanceEclCompletionResult result = service.completeCalculatedEclResults(BASE_DATE);

        assertThat(result.calculatedResultCount()).isEqualTo(7);
        assertThat(result.completedResultCount()).isEqualTo(7);
        assertThat(port.updated).isTrue();
    }

    @Test
    void completeCalculatedEclResults_returnsZeroWhenNoCalculatedRowsExist() {
        FakeAllowanceEclCompletionPort port = new FakeAllowanceEclCompletionPort(0, 0);
        AllowanceEclCompletionService service = new AllowanceEclCompletionService(port);

        AllowanceEclCompletionResult result = service.completeCalculatedEclResults(BASE_DATE);

        assertThat(result.calculatedResultCount()).isZero();
        assertThat(result.completedResultCount()).isZero();
        assertThat(port.updated).isFalse();
    }

    @Test
    void completeCalculatedEclResults_failsOnRowCountMismatch() {
        FakeAllowanceEclCompletionPort port = new FakeAllowanceEclCompletionPort(7, 6);
        AllowanceEclCompletionService service = new AllowanceEclCompletionService(port);

        assertThatThrownBy(() -> service.completeCalculatedEclResults(BASE_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("row count mismatch");

        assertThat(port.updated).isTrue();
    }

    private static class FakeAllowanceEclCompletionPort implements AllowanceEclCompletionPort {
        private final int calculatedCount;
        private final int completedCount;
        private boolean updated;

        private FakeAllowanceEclCompletionPort(int calculatedCount, int completedCount) {
            this.calculatedCount = calculatedCount;
            this.completedCount = completedCount;
        }

        @Override
        public int countCalculatedEclResults(LocalDate baseDate) {
            return calculatedCount;
        }

        @Override
        public int markCalculatedEclResultsCompleted(LocalDate baseDate) {
            updated = true;
            return completedCount;
        }
    }
}
