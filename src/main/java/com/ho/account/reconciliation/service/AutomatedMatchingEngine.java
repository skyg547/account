package com.ho.account.reconciliation.service;

import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.reconciliation.domain.BankStatement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 은행 거래 내역과 장부(GL) 항목을 자동으로 매칭하는 엔진
 */
@Component
public class AutomatedMatchingEngine {

    public static class MatchResult {
        private final BankStatement bankStatement;
        private final JournalDetail journalDetail;
        private final boolean isMatch;
        private final String matchReason;

        public MatchResult(BankStatement bankStatement, JournalDetail journalDetail, boolean isMatch,
                String matchReason) {
            this.bankStatement = bankStatement;
            this.journalDetail = journalDetail;
            this.isMatch = isMatch;
            this.matchReason = matchReason;
        }

        public BankStatement getBankStatement() {
            return bankStatement;
        }

        public JournalDetail getJournalDetail() {
            return journalDetail;
        }

        public boolean isMatch() {
            return isMatch;
        }

        public String getMatchReason() {
            return matchReason;
        }
    }

    /**
     * 은행 내역 리스트와 장부 내역 리스트를 대조하여 매칭 결과를 반환합니다.
     */
    public List<MatchResult> match(List<BankStatement> statements, List<JournalDetail> details) {
        List<MatchResult> results = new ArrayList<>();

        for (BankStatement stmt : statements) {
            boolean found = false;
            for (JournalDetail detail : details) {
                if (isMatch(stmt, detail)) {
                    results.add(new MatchResult(stmt, detail, true, "EXACT_DATE_AMOUNT_MATCH"));
                    found = true;
                    break;
                }
            }
            if (!found) {
                results.add(new MatchResult(stmt, null, false, "NO_MATCH_FOUND"));
            }
        }
        return results;
    }

    private boolean isMatch(BankStatement stmt, JournalDetail detail) {
        // 1. 금액 비교 (절대값 기준 - 통장은 입금/출금 구분, 장부는 차변/대변 구분)
        BigDecimal stmtAmount = stmt.getDepositAmount().compareTo(BigDecimal.ZERO) > 0 ? stmt.getDepositAmount()
                : stmt.getWithdrawalAmount();

        if (stmtAmount.compareTo(detail.getAmount()) != 0) {
            return false;
        }

        // 2. 날짜 비교 (전기일 기준, 통상 +- 1~3일 허용 가능하나 여기서는 일치로 한정)
        LocalDate stmtDate = stmt.getTransactionDate();
        LocalDate glDate = detail.getJournalEntry().getAccountingDate();

        if (!stmtDate.equals(glDate)) {
            // 퍼지 매칭: 날짜가 1일 차이인 경우도 허용하도록 확장 가능
            return false;
        }

        // 3. 적요/설명 비교 (간단한 포함 여부 확인)
        // String stmtDesc = stmt.getDescription() != null ? stmt.getDescription() : "";
        // String glDesc = detail.getDetailDescription() != null ?
        // detail.getDetailDescription() : "";

        return true;
    }
}
