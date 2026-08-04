package com.ho.account.reporting.core.domain.service;

import com.ho.account.reporting.core.domain.service.FinancialStatementEngine.AccountCategory;
import com.ho.account.reporting.core.domain.service.FinancialStatementEngine.TrialBalanceEntry;
import com.ho.account.reporting.domain.model.FinancialStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FinancialStatementEngine 단위 테스트")
class FinancialStatementEngineTest {

    private final FinancialStatementEngine engine = new FinancialStatementEngine();

    @Test
    @DisplayName("시산표 데이터로부터 손익계산서(I/S)의 총수익, 총비용, 당기순이익이 바르게 집계되는지 검증한다")
    void generateIncomeStatement() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);

        List<TrialBalanceEntry> trialBalance = List.of(
                new TrialBalanceEntry("4101", "이자수익", AccountCategory.REVENUE, new BigDecimal("500000.00"), new BigDecimal("400000.00")),
                new TrialBalanceEntry("4102", "수수료수익", AccountCategory.REVENUE, new BigDecimal("100000.00"), new BigDecimal("80000.00")),
                new TrialBalanceEntry("5101", "이자비용", AccountCategory.EXPENSE, new BigDecimal("200000.00"), new BigDecimal("150000.00")),
                new TrialBalanceEntry("5102", "판매관리비", AccountCategory.EXPENSE, new BigDecimal("150000.00"), new BigDecimal("120000.00"))
        );

        FinancialStatement is = engine.generateIncomeStatement("2026-IS-001", baseDate, trialBalance);

        assertThat(is.getType()).isEqualTo(FinancialStatement.StatementType.INCOME_STATEMENT);
        assertThat(is.getStatus()).isEqualTo(FinancialStatement.StatementStatus.FINAL);

        // 총 수익: 600,000 / 480,000
        // 총 비용: 350,000 / 270,000
        // 당기순이익: 250,000 / 210,000
        var netIncomeLine = is.getLines().stream().filter(l -> "IS_NET_INCOME".equals(l.getLineCode())).findFirst().orElseThrow();
        assertThat(netIncomeLine.getCurrentAmount()).isEqualByComparingTo("250000.00");
        assertThat(netIncomeLine.getPreviousAmount()).isEqualByComparingTo("210000.00");
    }

    @Test
    @DisplayName("시산표 데이터와 당기순이익으로부터 재무상태표(B/S)가 집계되고 자산=부채+자본 등식이 성립하는지 검증한다")
    void generateBalanceSheet() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);

        // Assets: 1,000,000 / Liabilities: 400,000 / Equity: 350,000 + Net Income 250,000 = 600,000 => Total L+E = 1,000,000
        List<TrialBalanceEntry> trialBalance = List.of(
                new TrialBalanceEntry("1010", "현금 및 현금성자산", AccountCategory.ASSET, new BigDecimal("1000000.00"), new BigDecimal("800000.00")),
                new TrialBalanceEntry("2010", "예금부채", AccountCategory.LIABILITY, new BigDecimal("400000.00"), new BigDecimal("300000.00")),
                new TrialBalanceEntry("3010", "자본금", AccountCategory.EQUITY, new BigDecimal("350000.00"), new BigDecimal("290000.00"))
        );

        BigDecimal netIncomeCurrent = new BigDecimal("250000.00");
        BigDecimal netIncomePrevious = new BigDecimal("210000.00");

        FinancialStatement bs = engine.generateBalanceSheet("2026-BS-001", baseDate, trialBalance, netIncomeCurrent, netIncomePrevious);

        assertThat(bs.getType()).isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        assertThat(bs.getStatus()).isEqualTo(FinancialStatement.StatementStatus.FINAL);

        var assetsLine = bs.getLines().stream().filter(l -> "BS_TOTAL_ASSETS".equals(l.getLineCode())).findFirst().orElseThrow();
        var equityLine = bs.getLines().stream().filter(l -> "BS_TOTAL_EQUITY".equals(l.getLineCode())).findFirst().orElseThrow();

        assertThat(assetsLine.getCurrentAmount()).isEqualByComparingTo("1000000.00");
        assertThat(equityLine.getCurrentAmount()).isEqualByComparingTo("600000.00");
    }

    @Test
    @DisplayName("자산과 부채+자본이 불일치하면 예외가 발생한다")
    void generateBalanceSheet_MismatchException() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);

        // Assets: 1,000,000 / Liabilities: 400,000 / Equity: 300,000 + Net Income 0 = 300,000 => Total L+E = 700,000 (불일치!)
        List<TrialBalanceEntry> trialBalance = List.of(
                new TrialBalanceEntry("1010", "현금", AccountCategory.ASSET, new BigDecimal("1000000.00"), BigDecimal.ZERO),
                new TrialBalanceEntry("2010", "부채", AccountCategory.LIABILITY, new BigDecimal("400000.00"), BigDecimal.ZERO),
                new TrialBalanceEntry("3010", "자본", AccountCategory.EQUITY, new BigDecimal("300000.00"), BigDecimal.ZERO)
        );

        assertThatThrownBy(() -> engine.generateBalanceSheet("2026-BS-MISMATCH", baseDate, trialBalance, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Accounting equation mismatch");
    }

    @Test
    @DisplayName("영업, 투자, 재무활동 현금흐름과 당기순이익으로부터 현금흐름표(C/F)가 정확히 집계되는지 검증한다")
    void generateCashFlowStatement() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);

        List<FinancialStatementEngine.CashFlowEntry> entries = List.of(
                new FinancialStatementEngine.CashFlowEntry("CF_OP_01", "매출채권 감소(증가)", FinancialStatementEngine.CashFlowActivityType.OPERATING, new BigDecimal("50000.00"), new BigDecimal("30000.00")),
                new FinancialStatementEngine.CashFlowEntry("CF_INV_01", "유형자산 취득", FinancialStatementEngine.CashFlowActivityType.INVESTING, new BigDecimal("-100000.00"), new BigDecimal("-80000.00")),
                new FinancialStatementEngine.CashFlowEntry("CF_FIN_01", "단기차입금 증가", FinancialStatementEngine.CashFlowActivityType.FINANCING, new BigDecimal("200000.00"), new BigDecimal("150000.00"))
        );

        BigDecimal netIncomeCurrent = new BigDecimal("250000.00");
        BigDecimal netIncomePrevious = new BigDecimal("210000.00");

        FinancialStatement cf = engine.generateCashFlowStatement("2026-CF-001", baseDate, netIncomeCurrent, netIncomePrevious, entries);

        assertThat(cf.getType()).isEqualTo(FinancialStatement.StatementType.CASH_FLOW_STATEMENT);
        assertThat(cf.getStatus()).isEqualTo(FinancialStatement.StatementStatus.FINAL);

        // Operating: NetIncome(250,000) + CF_OP_01(50,000) = 300,000 / Prev: 210,000 + 30,000 = 240,000
        // Investing: -100,000 / Prev: -80,000
        // Financing: 200,000 / Prev: 150,000
        // Net Cash Flow Total: 300,000 - 100,000 + 200,000 = 400,000 / Prev: 240,000 - 80,000 + 150,000 = 310,000
        var operatingTotal = cf.getLines().stream().filter(l -> "CF_TOTAL_OPERATING".equals(l.getLineCode())).findFirst().orElseThrow();
        var investingTotal = cf.getLines().stream().filter(l -> "CF_TOTAL_INVESTING".equals(l.getLineCode())).findFirst().orElseThrow();
        var financingTotal = cf.getLines().stream().filter(l -> "CF_TOTAL_FINANCING".equals(l.getLineCode())).findFirst().orElseThrow();
        var netTotal = cf.getLines().stream().filter(l -> "CF_NET_CASH_FLOW".equals(l.getLineCode())).findFirst().orElseThrow();

        assertThat(operatingTotal.getCurrentAmount()).isEqualByComparingTo("300000.00");
        assertThat(investingTotal.getCurrentAmount()).isEqualByComparingTo("-100000.00");
        assertThat(financingTotal.getCurrentAmount()).isEqualByComparingTo("200000.00");
        assertThat(netTotal.getCurrentAmount()).isEqualByComparingTo("400000.00");
        assertThat(netTotal.getPreviousAmount()).isEqualByComparingTo("310000.00");
    }
}
