package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 승인된 전표를 총계정원장(GL)과 보조원장(SL)에 기록할 불변 스냅샷으로 바꾸는 Aggregate입니다.
 *
 * <p>초보자 설명: 전표는 "거래를 승인하는 문서"이고 원장은 "승인된 거래를 오래 보관하는
 * 장부"입니다. 원장 저장 중에 원본 전표 라인이 바뀌면 GL과 SL이 서로 다른 금액을 가질 수
 * 있으므로, 전기 시작 시점의 값을 {@link Posting} 목록으로 한 번 복사해 두 경로가 같은
 * 스냅샷을 사용하게 합니다.</p>
 *
 * <p>이 Aggregate에는 JPA나 JDBC 코드가 없습니다. 애플리케이션은 이 객체만 output port로
 * 전달하고, 각 outbound adapter가 자신의 저장 기술에 맞게 변환합니다.</p>
 */
public final class GeneralLedger {

    private final Long journalEntryId;
    private final String slipNo;
    private final LocalDate accountingDate;
    private final String currencyCode;
    private final List<Posting> postings;

    private GeneralLedger(
            Long journalEntryId,
            String slipNo,
            LocalDate accountingDate,
            String currencyCode,
            List<Posting> postings) {
        this.journalEntryId = Objects.requireNonNull(journalEntryId, "전표 ID는 필수입니다.");
        this.slipNo = requireText(slipNo, "전표 번호");
        this.accountingDate = Objects.requireNonNull(accountingDate, "회계 반영일은 필수입니다.");
        this.currencyCode = requireText(currencyCode, "통화 코드").toUpperCase(Locale.ROOT);
        this.postings = List.copyOf(postings);
        if (this.postings.isEmpty()) {
            throw new IllegalArgumentException("원장에 기록할 전표 라인이 없습니다.");
        }
        validateTotals();
    }

    /**
     * 승인·저장된 전표만 원장 Aggregate로 승격합니다.
     */
    public static GeneralLedger fromApproved(JournalEntry journalEntry) {
        if (journalEntry == null) {
            throw new IllegalArgumentException("전표는 필수입니다.");
        }
        if (journalEntry.getStatus() != JournalEntryStatus.APPROVED) {
            throw new IllegalStateException("승인된 전표만 원장으로 전기할 수 있습니다.");
        }
        if (journalEntry.getId() == null) {
            throw new IllegalStateException("원장 전기 전에 전표가 먼저 저장되어야 합니다.");
        }

        journalEntry.validateBalance();
        List<Posting> postings = journalEntry.getDetails().stream()
                .map(detail -> Posting.from(journalEntry, detail))
                .toList();
        return new GeneralLedger(
                journalEntry.getId(),
                journalEntry.getSlipNo(),
                journalEntry.getAccountingDate(),
                journalEntry.getCurrencyCode(),
                postings);
    }

    private void validateTotals() {
        // Posting 한 건의 저장 정밀도와 여러 posting 합계의 정밀도는 다릅니다. 합계는
        // 별도 DECIMAL(19,2) 컬럼에 쓰지 않으므로 BigDecimal로 넓게 합산합니다.
        BigDecimal debitTotal = postings.stream()
                .map(Posting::debit)
                .map(Debit::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditTotal = postings.stream()
                .map(Posting::credit)
                .map(Credit::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal baseDebitTotal = postings.stream()
                .map(Posting::baseDebit)
                .map(Debit::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal baseCreditTotal = postings.stream()
                .map(Posting::baseCredit)
                .map(Credit::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalStateException("원장 거래통화 차변/대변 합계가 일치하지 않습니다.");
        }
        if (baseDebitTotal.compareTo(baseCreditTotal) != 0) {
            throw new IllegalStateException("원장 기준통화 차변/대변 합계가 일치하지 않습니다.");
        }
    }

    public Long journalEntryId() {
        return journalEntryId;
    }

    public String slipNo() {
        return slipNo;
    }

    public LocalDate accountingDate() {
        return accountingDate;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public List<Posting> postings() {
        return postings;
    }

    /**
     * GL과 SL adapter가 함께 소비하는 한 전표 라인의 불변 전기 스냅샷입니다.
     */
    public record Posting(
            Long journalDetailId,
            String accountCode,
            String businessPartnerCode,
            String departmentCode,
            String currencyCode,
            String fiscalYear,
            String fiscalPeriod,
            LocalDate postingDate,
            Debit debit,
            Credit credit,
            Debit baseDebit,
            Credit baseCredit,
            String summary,
            String lineageSourceType,
            String lineageSourceId) {

        public Posting {
            Objects.requireNonNull(journalDetailId, "저장된 전표 상세 ID는 필수입니다.");
            accountCode = requireText(accountCode, "계정 코드");
            currencyCode = requireText(currencyCode, "통화 코드").toUpperCase(Locale.ROOT);
            fiscalYear = requireText(fiscalYear, "회계연도");
            fiscalPeriod = requireText(fiscalPeriod, "회계기간");
            Objects.requireNonNull(postingDate, "전기일은 필수입니다.");
            Objects.requireNonNull(debit, "차변 VO는 필수입니다.");
            Objects.requireNonNull(credit, "대변 VO는 필수입니다.");
            Objects.requireNonNull(baseDebit, "기준통화 차변 VO는 필수입니다.");
            Objects.requireNonNull(baseCredit, "기준통화 대변 VO는 필수입니다.");

            if (debit.isPositive() == credit.isPositive()) {
                throw new IllegalArgumentException("한 원장 라인은 차변 또는 대변 한쪽만 양수여야 합니다.");
            }
            if (baseDebit.isPositive() == baseCredit.isPositive()) {
                throw new IllegalArgumentException("한 기준통화 원장 라인은 차변 또는 대변 한쪽만 양수여야 합니다.");
            }
            if (debit.isPositive() != baseDebit.isPositive()) {
                throw new IllegalArgumentException("거래통화와 기준통화의 차대 방향은 같아야 합니다.");
            }
        }

        private static Posting from(JournalEntry entry, JournalDetail detail) {
            if (detail.getId() == null) {
                throw new IllegalStateException("원장 전기 전에 전표 상세가 먼저 저장되어야 합니다.");
            }
            LocalDate accountingDate = entry.getAccountingDate();
            boolean debitSide = detail.getSide() == JournalSide.DEBIT;
            Debit debit = debitSide ? Debit.of(detail.getAmount()) : Debit.ZERO;
            Credit credit = debitSide ? Credit.ZERO : Credit.of(detail.getAmount());
            Debit baseDebit = debitSide ? Debit.of(detail.getBaseAmount()) : Debit.ZERO;
            Credit baseCredit = debitSide ? Credit.ZERO : Credit.of(detail.getBaseAmount());

            return new Posting(
                    detail.getId(),
                    detail.getAccountCode(),
                    detail.getBusinessPartnerCode(),
                    detail.getDepartmentCode(),
                    entry.getCurrencyCode(),
                    String.valueOf(accountingDate.getYear()),
                    String.format("%02d", accountingDate.getMonthValue()),
                    accountingDate,
                    debit,
                    credit,
                    baseDebit,
                    baseCredit,
                    detail.getDetailDescription(),
                    entry.getLineageSourceType(),
                    entry.getLineageSourceId());
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
