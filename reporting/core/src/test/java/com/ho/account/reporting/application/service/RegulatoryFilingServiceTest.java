package com.ho.account.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.reporting.application.port.in.SubmitRegulatoryFilingUseCase.SubmitCommand;
import com.ho.account.reporting.application.port.out.LoadDisclosureNoteMartPort;
import com.ho.account.reporting.application.port.out.LoadRegulatoryFilingPort;
import com.ho.account.reporting.application.port.out.LoadRegulatoryReportMappingPort;
import com.ho.account.reporting.application.port.out.LoadRegulatoryReportSubmissionPort;
import com.ho.account.reporting.application.port.out.StoreRegulatoryFilingPort;
import com.ho.account.reporting.application.port.out.SubmitRegulatoryFilingPort;
import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.NoteCategory;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import com.ho.account.reporting.domain.model.RegulatoryFilingPackage;
import com.ho.account.reporting.domain.model.RegulatoryFilingReceipt;
import com.ho.account.reporting.domain.model.RegulatoryReportMapping;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RegulatoryFilingServiceTest {

    @Test
    void submit_mapsReadySubmissionAndDisclosureMartToRegulatoryFiling() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        FinancialStatement statement = finalizedStatement(baseDate);
        RegulatoryReportSubmission submission = RegulatoryReportSubmission.ready(
                "SUB-001",
                statement,
                1,
                "maker",
                null,
                LocalDateTime.of(2026, 4, 1, 9, 0));
        DisclosureNoteMart mart = DisclosureNoteMart.fromStatement(
                statement,
                "maker",
                LocalDateTime.of(2026, 4, 1, 9, 5));
        CapturingFilingPort filingPort = new CapturingFilingPort();
        RegulatoryFilingService service = new RegulatoryFilingService(
                readySubmissionPort(submission),
                disclosureMartPort(mart),
                mappingPort(),
                localGateway(),
                filingPort,
                filingPort);

        RegulatoryFiling filing = service.submit(new SubmitCommand(
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate,
                "submitter",
                "FSS"));

        assertThat(filing.getSubmissionId()).isEqualTo("SUB-001");
        assertThat(filing.getTargetAgency()).isEqualTo("FSS");
        assertThat(filing.getStatus()).isEqualTo(RegulatoryFiling.FilingStatus.ACCEPTED);
        assertThat(filing.getRegulatorReceiptId()).isEqualTo("RECEIPT-001");
        assertThat(filing.getLines())
                .extracting(line -> line.fieldCode())
                .containsExactly("CASH_AND_CASH_EQUIVALENTS", "DEPOSIT_LIABILITIES");
        assertThat(filingPort.saved.get()).isSameAs(filing);
    }

    @Test
    void submit_rejectsWhenDisclosureMartIsMissing() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        RegulatoryFilingService service = new RegulatoryFilingService(
                readySubmissionPort(RegulatoryReportSubmission.ready(
                        "SUB-001",
                        finalizedStatement(baseDate),
                        1,
                        "maker",
                        null,
                        LocalDateTime.of(2026, 4, 1, 9, 0))),
                (type, date) -> Optional.empty(),
                mappingPort(),
                localGateway(),
                filing -> {
                },
                (type, date) -> Optional.empty());

        assertThatThrownBy(() -> service.submit(new SubmitCommand(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        baseDate,
                        "submitter",
                        "FSS")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Disclosure note mart is required before regulatory filing.");
    }

    @Test
    void submit_rejectsWhenRequiredMappingSourceIsMissing() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        FinancialStatement statement = finalizedStatement(baseDate);
        DisclosureNoteMart mart = DisclosureNoteMart.fromStatement(
                statement,
                "maker",
                LocalDateTime.of(2026, 4, 1, 9, 5));
        LoadRegulatoryReportMappingPort missingMappingPort = (type, date) -> List.of(new RegulatoryReportMapping(
                FinancialStatement.StatementType.BALANCE_SHEET,
                "FSS",
                "FSS_BS_DISCLOSURE",
                "MISSING_FIELD",
                "누락 필드",
                NoteCategory.RISK,
                "99",
                "MISSING_LINE",
                true,
                10,
                LocalDate.of(2020, 1, 1),
                null));
        RegulatoryFilingService service = new RegulatoryFilingService(
                readySubmissionPort(RegulatoryReportSubmission.ready(
                        "SUB-001",
                        statement,
                        1,
                        "maker",
                        null,
                        LocalDateTime.of(2026, 4, 1, 9, 0))),
                disclosureMartPort(mart),
                missingMappingPort,
                localGateway(),
                filing -> {
                },
                (type, date) -> Optional.empty());

        assertThatThrownBy(() -> service.submit(new SubmitCommand(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        baseDate,
                        "submitter",
                        "FSS")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Required regulatory source is missing");
    }

    private static LoadRegulatoryReportSubmissionPort readySubmissionPort(RegulatoryReportSubmission submission) {
        return (type, date) -> Optional.of(submission)
                .filter(value -> value.getStatementType() == type && value.getBaseDate().equals(date));
    }

    private static LoadDisclosureNoteMartPort disclosureMartPort(DisclosureNoteMart mart) {
        return (type, date) -> Optional.of(mart)
                .filter(value -> value.getStatementType() == type && value.getBaseDate().equals(date));
    }

    private static LoadRegulatoryReportMappingPort mappingPort() {
        return (type, date) -> List.of(
                new RegulatoryReportMapping(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        "FSS",
                        "FSS_BS_DISCLOSURE",
                        "CASH_AND_CASH_EQUIVALENTS",
                        "현금 및 현금성자산",
                        NoteCategory.CURRENCY,
                        "3",
                        "ASSET_CASH",
                        true,
                        10,
                        LocalDate.of(2020, 1, 1),
                        null),
                new RegulatoryReportMapping(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        "FSS",
                        "FSS_BS_DISCLOSURE",
                        "DEPOSIT_LIABILITIES",
                        "예수부채",
                        NoteCategory.MATURITY,
                        "8",
                        "LIABILITY_DEPOSIT",
                        true,
                        20,
                        LocalDate.of(2020, 1, 1),
                        null));
    }

    private static SubmitRegulatoryFilingPort localGateway() {
        return filingPackage -> new RegulatoryFilingReceipt(
                "RECEIPT-001",
                LocalDateTime.of(2026, 4, 1, 10, 0),
                "Accepted");
    }

    private static FinancialStatement finalizedStatement(LocalDateTime baseDate) {
        FinancialStatement statement = new FinancialStatement(
                "ST-001",
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate);
        statement.addLine(new ReportLine(
                "ASSET_CASH",
                "Cash",
                new BigDecimal("850000000"),
                new BigDecimal("700000000"),
                "3",
                1));
        statement.addLine(new ReportLine(
                "LIABILITY_DEPOSIT",
                "Demand deposits",
                new BigDecimal("500000000"),
                new BigDecimal("450000000"),
                "8",
                1));
        statement.finalizeStatement();
        return statement;
    }

    private static class CapturingFilingPort implements StoreRegulatoryFilingPort, LoadRegulatoryFilingPort {

        private final AtomicReference<RegulatoryFiling> saved = new AtomicReference<>();

        @Override
        public void save(RegulatoryFiling filing) {
            saved.set(filing);
        }

        @Override
        public Optional<RegulatoryFiling> findLatest(
                FinancialStatement.StatementType type,
                LocalDateTime baseDate) {
            return Optional.ofNullable(saved.get())
                    .filter(filing -> filing.getStatementType() == type && filing.getBaseDate().equals(baseDate));
        }
    }
}
