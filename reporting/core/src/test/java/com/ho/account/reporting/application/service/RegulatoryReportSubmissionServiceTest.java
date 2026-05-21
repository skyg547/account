package com.ho.account.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.reporting.application.port.in.SubmitRegulatoryReportUseCase.SubmitCommand;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.application.port.out.StoreRegulatoryReportSubmissionPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RegulatoryReportSubmissionServiceTest {

    @Test
    void submit_createsFirstReadyVersionFromFinalSnapshot() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        LoadReportHistoryPort historyPort = (type, date) -> Optional.of(finalizedStatement("ST-001", baseDate));
        CapturingSubmissionPort submissionPort = new CapturingSubmissionPort(1);
        RegulatoryReportSubmissionService service = new RegulatoryReportSubmissionService(historyPort, submissionPort);

        RegulatoryReportSubmission result = service.submit(new SubmitCommand(
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate,
                "tester",
                null));

        assertThat(result.getStatementId()).isEqualTo("ST-001");
        assertThat(result.getStatementType()).isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getSubmittedBy()).isEqualTo("tester");
        assertThat(result.getStatus()).isEqualTo(RegulatoryReportSubmission.SubmissionStatus.READY);
        assertThat(submissionPort.savedSubmission.get()).isSameAs(result);
    }

    @Test
    void submit_requiresCorrectionReasonFromSecondVersion() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        LoadReportHistoryPort historyPort = (type, date) -> Optional.of(finalizedStatement("ST-001", baseDate));
        RegulatoryReportSubmissionService service =
                new RegulatoryReportSubmissionService(historyPort, new CapturingSubmissionPort(2));

        assertThatThrownBy(() -> service.submit(new SubmitCommand(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        baseDate,
                        "tester",
                        " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("correctionReason is required for corrected submissions.");
    }

    @Test
    void submit_rejectsUnbalancedBalanceSheetTotalsWhenTotalLinesExist() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        FinancialStatement statement = new FinancialStatement(
                "ST-001",
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate);
        statement.addLine(new ReportLine(
                "TOTAL_ASSETS",
                "Total assets",
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                "",
                1));
        statement.addLine(new ReportLine(
                "TOTAL_LIABILITIES_EQUITY",
                "Total liabilities and equity",
                new BigDecimal("900"),
                BigDecimal.ZERO,
                "",
                1));
        statement.finalizeStatement();
        RegulatoryReportSubmissionService service =
                new RegulatoryReportSubmissionService((type, date) -> Optional.of(statement), new CapturingSubmissionPort(1));

        assertThatThrownBy(() -> service.submit(new SubmitCommand(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        baseDate,
                        "tester",
                        null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Balance sheet total mismatch");
    }

    private static FinancialStatement finalizedStatement(String statementId, LocalDateTime baseDate) {
        FinancialStatement statement = new FinancialStatement(
                statementId,
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate);
        statement.addLine(new ReportLine(
                "ASSET_CASH",
                "Cash",
                new BigDecimal("850000000"),
                new BigDecimal("700000000"),
                "3",
                1));
        statement.finalizeStatement();
        return statement;
    }

    private static class CapturingSubmissionPort implements StoreRegulatoryReportSubmissionPort {

        private final int nextVersion;
        private final AtomicReference<RegulatoryReportSubmission> savedSubmission = new AtomicReference<>();

        private CapturingSubmissionPort(int nextVersion) {
            this.nextVersion = nextVersion;
        }

        @Override
        public int nextVersion(FinancialStatement.StatementType type, LocalDateTime baseDate) {
            return nextVersion;
        }

        @Override
        public void save(RegulatoryReportSubmission submission) {
            savedSubmission.set(submission);
        }
    }
}
