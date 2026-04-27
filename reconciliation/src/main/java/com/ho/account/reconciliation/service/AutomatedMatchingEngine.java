package com.ho.account.reconciliation.service;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.reconciliation.domain.BankStatement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ?€??ê±°ë˜ ?´ì—­ê³??¥ë?(GL) ??ª©???ë™?¼ë¡œ ë§¤ì¹­?˜ëŠ” ?”ì§„
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
     * ?€???´ì—­ ë¦¬ìŠ¤?¸ì? ?¥ë? ?´ì—­ ë¦¬ìŠ¤?¸ë? ?€ì¡°í•˜??ë§¤ì¹­ ê²°ê³¼ë¥?ë°˜í™˜?©ë‹ˆ??
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
        // 1. ê¸ˆì•¡ ë¹„êµ (?ˆë?ê°?ê¸°ì? - ?µì¥?€ ?…ê¸ˆ/ì¶œê¸ˆ êµ¬ë¶„, ?¥ë???ì°¨ë?/?€ë³€ êµ¬ë¶„)
        BigDecimal stmtAmount = stmt.getDepositAmount().compareTo(BigDecimal.ZERO) > 0 ? stmt.getDepositAmount()
                : stmt.getWithdrawalAmount();

        if (stmtAmount.compareTo(detail.getAmount()) != 0) {
            return false;
        }

        // 2. ? ì§œ ë¹„êµ (?„ê¸°??ê¸°ì?, ?µìƒ +- 1~3???ˆìš© ê°€?¥í•˜???¬ê¸°?œëŠ” ?¼ì¹˜ë¡??œì •)
        LocalDate stmtDate = stmt.getTransactionDate();
        LocalDate glDate = detail.getJournalEntry().getAccountingDate();

        if (!stmtDate.equals(glDate)) {
            // ?¼ì? ë§¤ì¹­: ? ì§œê°€ 1??ì°¨ì´??ê²½ìš°???ˆìš©?˜ë„ë¡??•ì¥ ê°€??
            return false;
        }

        // 3. ?ìš”/?¤ëª… ë¹„êµ (ê°„ë‹¨???¬í•¨ ?¬ë? ?•ì¸)
        // String stmtDesc = stmt.getDescription() != null ? stmt.getDescription() : "";
        // String glDesc = detail.getDetailDescription() != null ?
        // detail.getDetailDescription() : "";

        return true;
    }
}
