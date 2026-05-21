package com.ho.account.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.reporting.application.port.in.DisclosureNoteMartUseCase.GenerateCommand;
import com.ho.account.reporting.application.port.out.LoadDisclosureNoteMartPort;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.application.port.out.StoreDisclosureNoteMartPort;
import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.NoteCategory;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DisclosureNoteMartServiceTest {

    @Test
    void generate_createsDisclosureEntriesFromFinalStatementNotes() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        LoadReportHistoryPort historyPort = (type, date) -> Optional.of(finalizedStatement(baseDate));
        CapturingDisclosureMartPort disclosureMartPort = new CapturingDisclosureMartPort();
        DisclosureNoteMartService service = new DisclosureNoteMartService(
                historyPort,
                disclosureMartPort,
                disclosureMartPort);

        DisclosureNoteMart mart = service.generate(new GenerateCommand(
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate,
                "tester"));

        assertThat(mart.getStatementId()).isEqualTo("ST-001");
        assertThat(mart.getEntries()).hasSize(3);
        assertThat(mart.getEntries())
                .extracting(entry -> entry.getNoteCategory())
                .contains(NoteCategory.CURRENCY, NoteCategory.MATURITY, NoteCategory.INTEREST_RATE);
        assertThat(disclosureMartPort.saved.get()).isSameAs(mart);
    }

    @Test
    void generate_requiresFinalStatementSnapshot() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        DisclosureNoteMartService service = new DisclosureNoteMartService(
                (type, date) -> Optional.empty(),
                mart -> {
                },
                (type, date) -> Optional.empty());

        assertThatThrownBy(() -> service.generate(new GenerateCommand(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        baseDate,
                        "tester")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Finalized statement snapshot is required before disclosure note mart generation.");
    }

    @Test
    void find_delegatesToLoadPort() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        DisclosureNoteMart mart = DisclosureNoteMart.fromStatement(
                finalizedStatement(baseDate),
                "tester",
                LocalDateTime.of(2026, 4, 1, 9, 0));
        CapturingDisclosureMartPort disclosureMartPort = new CapturingDisclosureMartPort();
        disclosureMartPort.saved.set(mart);
        DisclosureNoteMartService service = new DisclosureNoteMartService(
                (type, date) -> Optional.empty(),
                disclosureMartPort,
                disclosureMartPort);

        Optional<DisclosureNoteMart> result = service.find(new com.ho.account.reporting.application.port.in.DisclosureNoteMartUseCase.FindQuery(
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate));

        assertThat(result).containsSame(mart);
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
        statement.addLine(new ReportLine(
                "INTEREST_INCOME",
                "Interest income",
                new BigDecimal("12000000"),
                new BigDecimal("10000000"),
                "12",
                1));
        statement.finalizeStatement();
        return statement;
    }

    private static class CapturingDisclosureMartPort
            implements StoreDisclosureNoteMartPort, LoadDisclosureNoteMartPort {

        private final AtomicReference<DisclosureNoteMart> saved = new AtomicReference<>();

        @Override
        public void replace(DisclosureNoteMart mart) {
            saved.set(mart);
        }

        @Override
        public Optional<DisclosureNoteMart> find(FinancialStatement.StatementType type, LocalDateTime baseDate) {
            return Optional.ofNullable(saved.get())
                    .filter(mart -> mart.getStatementType() == type && mart.getBaseDate().equals(baseDate));
        }
    }
}
