package com.ho.account.reporting.core.domain.service;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.FinancialStatement.StatementStatus;
import com.ho.account.reporting.domain.model.FinancialStatement.StatementType;
import com.ho.account.reporting.domain.model.ReportLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * [도메인 서비스/계산기] 재무상태표(B/S) 및 손익계산서(I/S) 자동 집계 코어 엔진.
 *
 * 💡 [초보자를 위한 금융 회계 설명]
 * 이 클래스는 총계정원장(GL)의 계정과목별 잔액 시산표(Trial Balance)를 바탕으로 IFRS 재무제표를 집계합니다.
 *
 * 📌 [핵심 회계 등식 (Accounting Equation)]
 * 1. 재무상태표 (B/S): \( \text{자산(Assets)} = \text{부채(Liabilities)} + \text{자본(Equity)} \)
 * 2. 손익계산서 (I/S): \( \text{당기순이익(Net Income)} = \text{수익(Revenue)} - \text{비용(Expense)} \)
 * 3. 손익계산서의 당기순이익은 재무상태표의 자본(이익유보금)으로 정밀 이전되어 복식부기 대차가 맞아야 합니다.
 *
 * 🔧 [금융 정밀도 정책]
 * - 모든 연산은 `BigDecimal` 정밀 연산을 사용하며 원화 단위 반올림(`setScale(2, RoundingMode.HALF_UP)`)을 적용합니다.
 */
@Component
public class FinancialStatementEngine {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);

    /**
     * 계정과목 시산표 항목 레코드 (Trial Balance Entry)
     */
    public record TrialBalanceEntry(
            String accountCode,
            String accountName,
            AccountCategory category,
            BigDecimal currentBalance,
            BigDecimal previousBalance
    ) {
        public TrialBalanceEntry {
            if (accountCode == null || accountCode.isBlank()) {
                throw new IllegalArgumentException("accountCode is required.");
            }
            if (category == null) {
                throw new IllegalArgumentException("category is required.");
            }
            if (currentBalance == null) currentBalance = BigDecimal.ZERO;
            if (previousBalance == null) previousBalance = BigDecimal.ZERO;
        }
    }

    public enum AccountCategory {
        ASSET,       // 자산
        LIABILITY,   // 부채
        EQUITY,      // 자본
        REVENUE,     // 수익
        EXPENSE      // 비용
    }

    /**
     * 시산표 데이터를 바탕으로 손익계산서(Income Statement)를 집계합니다.
     */
    public FinancialStatement generateIncomeStatement(
            String statementId,
            LocalDateTime baseDate,
            List<TrialBalanceEntry> trialBalance) {

        if (statementId == null || statementId.isBlank()) {
            throw new IllegalArgumentException("statementId is required.");
        }
        if (baseDate == null) {
            throw new IllegalArgumentException("baseDate is required.");
        }
        List<TrialBalanceEntry> entries = (trialBalance != null) ? trialBalance : List.of();

        FinancialStatement statement = new FinancialStatement(statementId, StatementType.INCOME_STATEMENT, baseDate);

        BigDecimal totalRevenueCur = BigDecimal.ZERO;
        BigDecimal totalRevenuePrev = BigDecimal.ZERO;
        BigDecimal totalExpenseCur = BigDecimal.ZERO;
        BigDecimal totalExpensePrev = BigDecimal.ZERO;

        for (TrialBalanceEntry entry : entries) {
            if (entry.category() == AccountCategory.REVENUE) {
                totalRevenueCur = totalRevenueCur.add(entry.currentBalance(), MC);
                totalRevenuePrev = totalRevenuePrev.add(entry.previousBalance(), MC);
                statement.addLine(new ReportLine(
                        entry.accountCode(), entry.accountName(),
                        entry.currentBalance(), entry.previousBalance(), "REV", 3));
            } else if (entry.category() == AccountCategory.EXPENSE) {
                totalExpenseCur = totalExpenseCur.add(entry.currentBalance(), MC);
                totalExpensePrev = totalExpensePrev.add(entry.previousBalance(), MC);
                statement.addLine(new ReportLine(
                        entry.accountCode(), entry.accountName(),
                        entry.currentBalance(), entry.previousBalance(), "EXP", 3));
            }
        }

        BigDecimal netIncomeCur = totalRevenueCur.subtract(totalExpenseCur, MC).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netIncomePrev = totalRevenuePrev.subtract(totalExpensePrev, MC).setScale(2, RoundingMode.HALF_UP);

        statement.addLine(new ReportLine("IS_TOTAL_REVENUE", "총 수익", totalRevenueCur.setScale(2, RoundingMode.HALF_UP), totalRevenuePrev.setScale(2, RoundingMode.HALF_UP), null, 1));
        statement.addLine(new ReportLine("IS_TOTAL_EXPENSE", "총 비용", totalExpenseCur.setScale(2, RoundingMode.HALF_UP), totalExpensePrev.setScale(2, RoundingMode.HALF_UP), null, 1));
        statement.addLine(new ReportLine("IS_NET_INCOME", "당기순이익", netIncomeCur, netIncomePrev, null, 1));

        statement.finalizeStatement();
        return statement;
    }

    /**
     * 시산표 데이터와 손익계산서 당기순이익을 바탕으로 재무상태표(Balance Sheet)를 집계합니다.
     */
    public FinancialStatement generateBalanceSheet(
            String statementId,
            LocalDateTime baseDate,
            List<TrialBalanceEntry> trialBalance,
            BigDecimal netIncomeCurrent,
            BigDecimal netIncomePrevious) {

        if (statementId == null || statementId.isBlank()) {
            throw new IllegalArgumentException("statementId is required.");
        }
        if (baseDate == null) {
            throw new IllegalArgumentException("baseDate is required.");
        }
        List<TrialBalanceEntry> entries = (trialBalance != null) ? trialBalance : List.of();

        FinancialStatement statement = new FinancialStatement(statementId, StatementType.BALANCE_SHEET, baseDate);

        BigDecimal totalAssetsCur = BigDecimal.ZERO;
        BigDecimal totalAssetsPrev = BigDecimal.ZERO;
        BigDecimal totalLiabilitiesCur = BigDecimal.ZERO;
        BigDecimal totalLiabilitiesPrev = BigDecimal.ZERO;
        BigDecimal totalEquityCur = BigDecimal.ZERO;
        BigDecimal totalEquityPrev = BigDecimal.ZERO;

        for (TrialBalanceEntry entry : entries) {
            switch (entry.category()) {
                case ASSET -> {
                    totalAssetsCur = totalAssetsCur.add(entry.currentBalance(), MC);
                    totalAssetsPrev = totalAssetsPrev.add(entry.previousBalance(), MC);
                    statement.addLine(new ReportLine(entry.accountCode(), entry.accountName(), entry.currentBalance(), entry.previousBalance(), "AST", 3));
                }
                case LIABILITY -> {
                    totalLiabilitiesCur = totalLiabilitiesCur.add(entry.currentBalance(), MC);
                    totalLiabilitiesPrev = totalLiabilitiesPrev.add(entry.previousBalance(), MC);
                    statement.addLine(new ReportLine(entry.accountCode(), entry.accountName(), entry.currentBalance(), entry.previousBalance(), "LIA", 3));
                }
                case EQUITY -> {
                    totalEquityCur = totalEquityCur.add(entry.currentBalance(), MC);
                    totalEquityPrev = totalEquityPrev.add(entry.previousBalance(), MC);
                    statement.addLine(new ReportLine(entry.accountCode(), entry.accountName(), entry.currentBalance(), entry.previousBalance(), "EQU", 3));
                }
                default -> { /* 손익계산서 항목 제외 */ }
            }
        }

        // 손익계산서의 당기순이익을 자본(이익유보금)에 합산
        BigDecimal safeNetIncomeCur = (netIncomeCurrent != null) ? netIncomeCurrent : BigDecimal.ZERO;
        BigDecimal safeNetIncomePrev = (netIncomePrevious != null) ? netIncomePrevious : BigDecimal.ZERO;

        totalEquityCur = totalEquityCur.add(safeNetIncomeCur, MC).setScale(2, RoundingMode.HALF_UP);
        totalEquityPrev = totalEquityPrev.add(safeNetIncomePrev, MC).setScale(2, RoundingMode.HALF_UP);

        statement.addLine(new ReportLine("BS_TOTAL_ASSETS", "자산 총계", totalAssetsCur.setScale(2, RoundingMode.HALF_UP), totalAssetsPrev.setScale(2, RoundingMode.HALF_UP), null, 1));
        statement.addLine(new ReportLine("BS_TOTAL_LIABILITIES", "부채 총계", totalLiabilitiesCur.setScale(2, RoundingMode.HALF_UP), totalLiabilitiesPrev.setScale(2, RoundingMode.HALF_UP), null, 1));
        statement.addLine(new ReportLine("BS_TOTAL_EQUITY", "자본 총계 (당기순이익 포함)", totalEquityCur, totalEquityPrev, null, 1));

        // 복식부기 등식 검증: 자산 == 부채 + 자본
        BigDecimal liabilitiesAndEquityCur = totalLiabilitiesCur.add(totalEquityCur, MC).setScale(2, RoundingMode.HALF_UP);
        if (totalAssetsCur.compareTo(liabilitiesAndEquityCur) != 0) {
            throw new IllegalStateException(String.format(
                    "Accounting equation mismatch! Assets: %s, Liabilities+Equity: %s", totalAssetsCur, liabilitiesAndEquityCur));
        }

        statement.finalizeStatement();
        return statement;
    }
}
