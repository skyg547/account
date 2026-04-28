package com.ho.account.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.reporting.application.port.in.GenerateStatementUseCase.GenerateCommand;
import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReportingServiceTest {

    @Test
    void generate_balanceSheet_containsCurrentAndPreviousCashLine() {
        LoadLedgerPort ledgerPort = baseDate -> Map.of(
                "101", new BigDecimal("500000000"),
                "102", new BigDecimal("350000000"));

        LoadReportHistoryPort historyPort = (type, date) -> {
            FinancialStatement previous = new FinancialStatement("PREV-001", type, date);
            previous.addLine(new ReportLine(
                    "ASSET_CASH",
                    "Cash",
                    new BigDecimal("700000000"),
                    BigDecimal.ZERO,
                    "3",
                    1));
            previous.finalizeStatement();
            return Optional.of(previous);
        };

        ReportingService service = new ReportingService(ledgerPort, historyPort);
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);

        FinancialStatement result = service.generate(
                new GenerateCommand(FinancialStatement.StatementType.BALANCE_SHEET, baseDate, "tester"));

        assertThat(result.getStatus()).isEqualTo(FinancialStatement.StatementStatus.FINAL);
        assertThat(result.getLines()).hasSize(1);
        ReportLine line = result.getLines().get(0);
        assertThat(line.getLineCode()).isEqualTo("ASSET_CASH");
        assertThat(line.getCurrentAmount()).isEqualByComparingTo("850000000");
        assertThat(line.getPreviousAmount()).isEqualByComparingTo("700000000");
    }
}
