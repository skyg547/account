package com.ho.account.masterdata.batch.application;

import com.ho.account.masterdata.core.application.pipeline.MasterDataValidityReportPipeline;
import com.ho.account.masterdata.core.application.port.out.MasterDataValidityStatisticsPort;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterDataBatchOrchestratorTest {

    @Test
    void createsDailyValidityReportFromDatabaseStatisticsPort() {
        LocalDate asOfDate = LocalDate.of(2026, 4, 20);
        RecordingStatisticsPort statisticsPort = new RecordingStatisticsPort();
        MasterDataValidityReportPipeline pipeline = new MasterDataValidityReportPipeline(statisticsPort);
        MasterDataBatchOrchestrator orchestrator = new MasterDataBatchOrchestrator(pipeline);

        MasterDataBatchReport report = orchestrator.createDailyValidityReport(asOfDate);

        assertThat(statisticsPort.requestedDate).isEqualTo(asOfDate);
        assertThat(report.asOfDate()).isEqualTo(asOfDate);
        assertThat(report.activeAccountSubjects()).isEqualTo(11);
        assertThat(report.activeDepartments()).isEqualTo(7);
        assertThat(report.activeProducts()).isEqualTo(5);
        assertThat(report.activeBusinessPartners()).isEqualTo(13);
    }

    @Test
    void rejectsMissingAsOfDateForReproducibleBatchExecution() {
        MasterDataValidityReportPipeline pipeline = new MasterDataValidityReportPipeline(
                new RecordingStatisticsPort());
        MasterDataBatchOrchestrator orchestrator = new MasterDataBatchOrchestrator(pipeline);

        assertThatThrownBy(() -> orchestrator.createDailyValidityReport(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("asOfDate is required");
    }

    private static final class RecordingStatisticsPort implements MasterDataValidityStatisticsPort {
        private LocalDate requestedDate;

        @Override
        public long countActiveAccountSubjects(LocalDate asOfDate) {
            requestedDate = asOfDate;
            return 11;
        }

        @Override
        public long countActiveDepartments(LocalDate asOfDate) {
            assertSameDate(asOfDate);
            return 7;
        }

        @Override
        public long countActiveProducts(LocalDate asOfDate) {
            assertSameDate(asOfDate);
            return 5;
        }

        @Override
        public long countActiveBusinessPartners(LocalDate asOfDate) {
            assertSameDate(asOfDate);
            return 13;
        }

        private void assertSameDate(LocalDate asOfDate) {
            if (!asOfDate.equals(requestedDate)) {
                throw new AssertionError("All statistics must use the same asOfDate.");
            }
        }
    }
}