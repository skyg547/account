package com.ho.account.reconciliation.service;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.reconciliation.domain.BankStatement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Matches bank statement rows with journal detail summaries.
 * 금액/일자 외에도 전표번호, 적요 유사도 등 복합 조건을 지원하도록 고도화되었습니다.
 */
@Component
public class AutomatedMatchingEngine {

    public static final String EXACT_DATE_AMOUNT_MATCH = "EXACT_DATE_AMOUNT_MATCH";
    public static final String TOLERANCE_DATE_AMOUNT_MATCH = "TOLERANCE_DATE_AMOUNT_MATCH";
    public static final String COMPLEX_DESCRIPTION_MATCH = "COMPLEX_DESCRIPTION_MATCH";
    public static final String SLIP_NO_MATCH = "SLIP_NO_MATCH";
    public static final String ACCOUNT_NO_MATCH = "ACCOUNT_NO_MATCH";
    public static final String NO_MATCH_FOUND = "NO_MATCH_FOUND";

    public static class MatchOptions {
        private final BigDecimal amountTolerance;
        private final long dateToleranceDays;
        private final boolean useDescriptionMatch;
        private final boolean useSlipNoMatch;
        private final boolean useAccountNoMatch;

        private MatchOptions(BigDecimal amountTolerance, long dateToleranceDays,
                boolean useDescriptionMatch, boolean useSlipNoMatch, boolean useAccountNoMatch) {
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
            this.useDescriptionMatch = useDescriptionMatch;
            this.useSlipNoMatch = useSlipNoMatch;
            this.useAccountNoMatch = useAccountNoMatch;
        }

        public static MatchOptions exact() {
            return new MatchOptions(BigDecimal.ZERO, 0, false, false, false);
        }

        public static MatchOptions of(BigDecimal amountTolerance, long dateToleranceDays) {
            return new MatchOptions(amountTolerance, dateToleranceDays, false, false, false);
        }

        public static MatchOptions complex(BigDecimal amountTolerance, long dateToleranceDays,
                boolean description, boolean slipNo, boolean accountNo) {
            return new MatchOptions(amountTolerance, dateToleranceDays, description, slipNo, accountNo);
        }

        public BigDecimal getAmountTolerance() {
            return amountTolerance;
        }

        public long getDateToleranceDays() {
            return dateToleranceDays;
        }

        public boolean isUseDescriptionMatch() {
            return useDescriptionMatch;
        }

        public boolean isUseSlipNoMatch() {
            return useSlipNoMatch;
        }

        public boolean isUseAccountNoMatch() {
            return useAccountNoMatch;
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
                String matchReason = resolveMatchReason(stmt, detail, options);
                if (matchReason != null) {
                    results.add(new MatchResult(stmt, detail, true, matchReason));
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

    private String resolveMatchReason(BankStatement stmt, JournalDetailSummary detail, MatchOptions options) {
        BigDecimal stmtAmount = statementAmount(stmt);
        BigDecimal detailAmount = detailAmount(detail);
        if (stmtAmount == null || detailAmount == null) {
            return null;
        }

        BigDecimal amountDifference = stmtAmount.subtract(detailAmount).abs();
        if (amountDifference.compareTo(options.getAmountTolerance()) > 0) {
            return null;
        }

        if (options.isUseSlipNoMatch() && containsNormalized(stmt.getDescription(), detail.getSlipNo())) {
            return SLIP_NO_MATCH;
        }

        if (options.isUseAccountNoMatch() && sameAccountNo(stmt.getAccountNo(), detail.getAccountNo())) {
            return ACCOUNT_NO_MATCH;
        }

        if (options.isUseDescriptionMatch()
                && (containsNormalized(stmt.getDescription(), detail.getDetailDescription())
                || containsNormalized(stmt.getDescription(), detail.getHeaderDescription()))) {
            return COMPLEX_DESCRIPTION_MATCH;
        }

        LocalDate stmtDate = stmt.getTransactionDate();
        LocalDate glDate = detail.getAccountingDate();
        if (stmtDate == null || glDate == null) {
            return null;
        }

        long dateDifference = Math.abs(ChronoUnit.DAYS.between(stmtDate, glDate));
        if (dateDifference > options.getDateToleranceDays()) {
            return null;
        }

        return amountDifference.compareTo(BigDecimal.ZERO) == 0 && dateDifference == 0
                ? EXACT_DATE_AMOUNT_MATCH
                : TOLERANCE_DATE_AMOUNT_MATCH;
    }

    private boolean containsNormalized(String source, String token) {
        String normalizedSource = normalizeText(source);
        String normalizedToken = normalizeText(token);
        return normalizedSource != null && normalizedToken != null
                && normalizedSource.contains(normalizedToken);
    }

    private boolean sameAccountNo(String left, String right) {
        String normalizedLeft = normalizeAccountNo(left);
        String normalizedRight = normalizeAccountNo(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeAccountNo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
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
