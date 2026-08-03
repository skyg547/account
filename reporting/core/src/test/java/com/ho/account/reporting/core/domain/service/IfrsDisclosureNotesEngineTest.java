package com.ho.account.reporting.core.domain.service;

import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IfrsDisclosureNotesEngine 단위 테스트")
class IfrsDisclosureNotesEngineTest {

    private final IfrsDisclosureNotesEngine engine = new IfrsDisclosureNotesEngine();

    @Test
    @DisplayName("DisclosureNoteMart로부터 주석 번호별 당기 및 전기 금액 합계가 바르게 집계되는지 검증한다")
    void summarizeDisclosureNotes() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        FinancialStatement statement = new FinancialStatement("2026-BS-TEST", FinancialStatement.StatementType.BALANCE_SHEET, baseDate);

        statement.addLine(new ReportLine("1010", "현금", new BigDecimal("100.00"), new BigDecimal("80.00"), "NOTE-01", 3));
        statement.addLine(new ReportLine("1020", "예금", new BigDecimal("200.00"), new BigDecimal("150.00"), "NOTE-01", 3));
        statement.addLine(new ReportLine("2010", "차입금", new BigDecimal("150.00"), new BigDecimal("100.00"), "NOTE-02", 3));

        statement.finalizeStatement();

        DisclosureNoteMart mart = DisclosureNoteMart.fromStatement(statement, "TEST_USER", LocalDateTime.now());

        Map<String, IfrsDisclosureNotesEngine.NoteSummary> summaryMap = engine.summarizeDisclosureNotes(mart);

        assertThat(summaryMap).hasSize(2);
        assertThat(summaryMap).containsKey("NOTE-01");
        assertThat(summaryMap).containsKey("NOTE-02");

        // NOTE-01: 현금 100 + 예금 200 = 300 / 전기 80 + 150 = 230
        IfrsDisclosureNotesEngine.NoteSummary note1 = summaryMap.get("NOTE-01");
        assertThat(note1.itemCounts()).isEqualTo(2);
        assertThat(note1.totalCurrentAmount()).isEqualByComparingTo("300.00");
        assertThat(note1.totalPreviousAmount()).isEqualByComparingTo("230.00");
    }
}
