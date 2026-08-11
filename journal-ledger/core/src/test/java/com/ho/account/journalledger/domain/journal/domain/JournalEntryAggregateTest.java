package com.ho.account.journalledger.domain.journal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JournalEntryAggregateTest {

    @Test
    @DisplayName("JournalEntry가 상세 라인의 소유권을 설정하고 외부 컬렉션 변경을 차단한다.")
    void ownsJournalDetailsAsAggregateChildren() {
        JournalEntry entry = new JournalEntry();
        JournalDetail debit = detail(JournalSide.DEBIT, "10100", "10.00", "10.00");
        JournalDetail credit = detail(JournalSide.CREDIT, "40100", "10.00", "10.00");

        entry.setDetails(List.of(debit, credit));

        assertThat(debit.getJournalEntry()).isSameAs(entry);
        assertThat(credit.getJournalEntry()).isSameAs(entry);
        assertThatThrownBy(() -> entry.getDetails().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("거래통화가 같아도 기준통화 차대가 다르면 승인을 차단한다.")
    void rejectsBaseCurrencyImbalance() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "100.00", "135000.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100", "100.00", "134999.99"));

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기준통화");
    }

    @Test
    @DisplayName("차변 또는 대변 한쪽만 있는 목록은 합계가 우연히 0이어도 전표가 될 수 없다.")
    void requiresBothAccountingSides() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "10.00", "10.00"));
        entry.addDetail(detail(JournalSide.DEBIT, "10200", "10.00", "10.00"));

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("차변과 대변");
    }

    @Test
    @DisplayName("전표 작성일(slipDate)이 누락되면 도메인 불변성 검증 시 예외가 발생한다.")
    void rejectsMissingSlipDateInvariants() {
        JournalEntry entry = new JournalEntry();
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "10.00", "10.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100", "10.00", "10.00"));

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("전표 작성일");
    }

    @Test
    @DisplayName("정상적인 헤더 및 차대변 라인이 설정된 전표는 불변성 검증을 통과한다.")
    void passesValidInvariants() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "10.00", "10.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100", "10.00", "10.00"));

        entry.validateInvariants();

        assertThat(entry.getAccountingDate()).isEqualTo(entry.getSlipDate());
    }

    private JournalDetail detail(
            JournalSide side,
            String accountCode,
            String amount,
            String baseAmount) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(baseAmount));
        return detail;
    }
}
