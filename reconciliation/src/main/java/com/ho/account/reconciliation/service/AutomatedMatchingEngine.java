package com.ho.account.reconciliation.service;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.reconciliation.domain.BankStatement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ???嫄곕옒 ?댁뿭怨??λ?(GL) ??ぉ???먮룞?쇰줈 留ㅼ묶?섎뒗 ?붿쭊
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
     * ????댁뿭 由ъ뒪?몄? ?λ? ?댁뿭 由ъ뒪?몃? ?議고븯??留ㅼ묶 寃곌낵瑜?諛섑솚?⑸땲??
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
        // 1. 湲덉븸 鍮꾧탳 (?덈?媛?湲곗? - ?듭옣? ?낃툑/異쒓툑 援щ텇, ?λ???李⑤?/?蹂 援щ텇)
        BigDecimal stmtAmount = stmt.getDepositAmount().compareTo(BigDecimal.ZERO) > 0 ? stmt.getDepositAmount()
                : stmt.getWithdrawalAmount();

        if (stmtAmount.compareTo(detail.getAmount()) != 0) {
            return false;
        }

        // 2. ?좎쭨 鍮꾧탳 (?꾧린??湲곗?, ?듭긽 +- 1~3???덉슜 媛?ν븯???ш린?쒕뒗 ?쇱튂濡??쒖젙)
        LocalDate stmtDate = stmt.getTransactionDate();
        LocalDate glDate = detail.getJournalEntry().getAccountingDate();

        if (!stmtDate.equals(glDate)) {
            // ?쇱? 留ㅼ묶: ?좎쭨媛 1??李⑥씠??寃쎌슦???덉슜?섎룄濡??뺤옣 媛??
            return false;
        }

        // 3. ?곸슂/?ㅻ챸 鍮꾧탳 (媛꾨떒???ы븿 ?щ? ?뺤씤)
        // String stmtDesc = stmt.getDescription() != null ? stmt.getDescription() : "";
        // String glDesc = detail.getDetailDescription() != null ?
        // detail.getDetailDescription() : "";

        return true;
    }
}
