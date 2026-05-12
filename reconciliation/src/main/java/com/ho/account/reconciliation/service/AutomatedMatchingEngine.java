package com.ho.account.reconciliation.service;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.reconciliation.domain.BankStatement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Matches bank statement rows with journal detail summaries.
 */
@Component
public class AutomatedMatchingEngine {

    public static final String EXACT_DATE_AMOUNT_MATCH = "EXACT_DATE_AMOUNT_MATCH";
    public static final String TOLERANCE_DATE_AMOUNT_MATCH = "TOLERANCE_DATE_AMOUNT_MATCH";
    public static final String NO_MATCH_FOUND = "NO_MATCH_FOUND";

    public static class MatchOptions {
        private final BigDecimal amountTolerance;
        private final long dateToleranceDays;

        private MatchOptions(BigDecimal amountTolerance, long dateToleranceDays) {
            if (amountTolerance == null) {
                throw new IllegalArgumentException("amountTolerance must not be null");
            }
            if (amountTolerance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("amountTolerance must not be negative");
            }
            if (dateToleranceDays < 0) {
                throw new IllegalArgumentException("dateToleranceDays must not be negative");
            }
            this.amountTolerance = amountTolerance;
            this.dateToleranceDays = dateToleranceDays;
        }

        public static MatchOptions exact() {
            return new MatchOptions(BigDecimal.ZERO, 0);
        }

        public static MatchOptions of(BigDecimal amountTolerance, long dateToleranceDays) {
            return new MatchOptions(amountTolerance, dateToleranceDays);
        }

        public BigDecimal getAmountTolerance() {
            return amountTolerance;
        }

        public long getDateToleranceDays() {
            return dateToleranceDays;
        }
    }

    public static class MatchResult {
        private final BankStatement bankStatement;
        private final JournalDetailSummary journalDetail;
        private final boolean isMatch;
        private final String matchReason;

        public MatchResult(BankStatement bankStatement, JournalDetailSummary journalDetail, boolean isMatch,
                String matchReason) {
            this.bankStatement = bankStatement;
            this.journalDetail = journalDetail;
            this.isMatch = isMatch;
            this.matchReason = matchReason;
        }

        public BankStatement getBankStatement() {
            return bankStatement;
        }

        public JournalDetailSummary getJournalDetail() {
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
     * Matches with exact amount and exact date to preserve legacy behavior.
     */
    public List<MatchResult> match(List<BankStatement> statements, List<JournalDetailSummary> details) {
        return match(statements, details, MatchOptions.exact());
    }

    public List<MatchResult> match(List<BankStatement> statements, List<JournalDetailSummary> details,
            MatchOptions options) {
        Objects.requireNonNull(statements, "statements must not be null");
        Objects.requireNonNull(details, "details must not be null");
        Objects.requireNonNull(options, "options must not be null");

        List<MatchResult> results = new ArrayList<>();

        for (BankStatement stmt : statements) {
            boolean found = false;
            for (JournalDetailSummary detail : details) {
                if (isMatch(stmt, detail, options)) {
                    results.add(new MatchResult(stmt, detail, true, matchReason(stmt, detail)));
                    found = true;
                    break;
                }
            }
            if (!found) {
                results.add(new MatchResult(stmt, null, false, NO_MATCH_FOUND));
            }
        }
        return results;
    }

    private boolean isMatch(BankStatement stmt, JournalDetailSummary detail, MatchOptions options) {
        BigDecimal stmtAmount = statementAmount(stmt);
        BigDecimal detailAmount = detailAmount(detail);
        if (stmtAmount == null || detailAmount == null) {
            return false;
        }

        BigDecimal amountDifference = stmtAmount.subtract(detailAmount).abs();
        if (amountDifference.compareTo(options.getAmountTolerance()) > 0) {
            return false;
        }

        LocalDate stmtDate = stmt.getTransactionDate();
        LocalDate glDate = detail.getAccountingDate();
        if (stmtDate == null || glDate == null) {
            return false;
        }

        long dateDifference = Math.abs(ChronoUnit.DAYS.between(stmtDate, glDate));
        return dateDifference <= options.getDateToleranceDays();
    }

    private String matchReason(BankStatement stmt, JournalDetailSummary detail) {
        BigDecimal stmtAmount = statementAmount(stmt);
        BigDecimal detailAmount = detailAmount(detail);
        LocalDate stmtDate = stmt.getTransactionDate();
        LocalDate glDate = detail.getAccountingDate();
        if (stmtAmount != null && detailAmount != null && stmtAmount.compareTo(detailAmount) == 0
                && stmtDate != null && stmtDate.equals(glDate)) {
            return EXACT_DATE_AMOUNT_MATCH;
        }
        return TOLERANCE_DATE_AMOUNT_MATCH;
    }

    private BigDecimal statementAmount(BankStatement statement) {
        BigDecimal depositAmount = zeroIfNull(statement.getDepositAmount());
        BigDecimal withdrawalAmount = zeroIfNull(statement.getWithdrawalAmount());
        if (depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            return depositAmount;
        }
        return withdrawalAmount;
    }

    private BigDecimal detailAmount(JournalDetailSummary detail) {
        return detail.getBaseAmount() != null ? detail.getBaseAmount() : detail.getAmount();
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
