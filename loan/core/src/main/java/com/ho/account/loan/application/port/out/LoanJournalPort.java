package com.ho.account.loan.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 대출 회계 이벤트를 전표 모듈로 전달하는 출력 포트.
 *
 * <p>Loan은 JournalEntry 엔티티를 소유하지 않습니다. 전표 생성 결과는 추적에 필요한 ID와 전표번호만
 * 보관하며, 승인/전기 절차는 어댑터가 journal-ledger 유즈케이스에 위임합니다.
 */
public interface LoanJournalPort {

    PostedJournal post(LoanJournalCommand command);

    record LoanJournalCommand(
            LocalDate accountingDate,
            String description,
            String actor,
            String lineageSourceType,
            String lineageSourceId,
            String currencyCode,
            List<LoanJournalLine> lines) {

        public LoanJournalCommand {
            if (accountingDate == null) {
                throw new IllegalArgumentException("accountingDate is required.");
            }
            description = requireText(description, "description");
            actor = requireText(actor, "actor");
            lineageSourceType = requireText(lineageSourceType, "lineageSourceType");
            lineageSourceId = requireText(lineageSourceId, "lineageSourceId");
            currencyCode = requireText(currencyCode, "currencyCode").toUpperCase(Locale.ROOT);
            if (currencyCode.length() != 3) {
                throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code.");
            }
            lines = lines == null ? List.of() : List.copyOf(lines);
            if (lines.size() < 2) {
                throw new IllegalArgumentException("Loan journal requires at least two lines.");
            }
            BigDecimal debits = total(lines, "DEBIT");
            BigDecimal credits = total(lines, "CREDIT");
            if (debits.compareTo(credits) != 0) {
                throw new IllegalArgumentException("Loan journal debits and credits must balance.");
            }
        }
    }

    record LoanJournalLine(String side, String accountCode, BigDecimal amount, String description) {
        public LoanJournalLine {
            side = requireText(side, "side");
            if (!side.equals("DEBIT") && !side.equals("CREDIT")) {
                throw new IllegalArgumentException("side must be DEBIT or CREDIT.");
            }
            accountCode = requireText(accountCode, "accountCode");
            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException("amount must be positive.");
            }
        }
    }

    record PostedJournal(Long journalEntryId, String slipNo) {
        public PostedJournal {
            if (journalEntryId == null || journalEntryId < 1) {
                throw new IllegalArgumentException("journalEntryId must be positive.");
            }
            slipNo = requireText(slipNo, "slipNo");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private static BigDecimal total(List<LoanJournalLine> lines, String side) {
        return lines.stream()
                .filter(line -> side.equals(line.side()))
                .map(LoanJournalLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
